package earl.client.display.svg.edit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.vectomatic.dom.svg.OMElement;
import org.vectomatic.dom.svg.OMSVGDocument;
import org.vectomatic.dom.svg.OMSVGLineElement;
import org.vectomatic.dom.svg.impl.SVGElement;
import org.vectomatic.dom.svg.impl.SVGSVGElement;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.HasMouseDownHandlers;
import com.google.gwt.event.dom.client.HasMouseMoveHandlers;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;

import earl.client.data.Board;
import earl.client.data.Hex;
import earl.client.display.handler.BasicDisplayHandler;
import earl.client.display.svg.SVGDisplay2;
import earl.client.op.Position;
import earl.client.utils.AbstractCallback;

public class EditDisplay extends SVGDisplay2 implements MouseMoveHandler, KeyPressHandler, MouseDownHandler {
	private String color = "black";
	private long id = 1;
	private Element source = null;
	private final Stack<Element> path = new Stack<Element>();
	private final OMSVGDocument doc;

	public EditDisplay(SVGSVGElement svg) {
		super(svg, new BasicDisplayHandler());
		doc = OMElement.convert(svg.getOwnerDocument());
		RootPanel.get().addDomHandler(this, KeyPressEvent.getType());
		loadAll();
	}

	@Override
	protected void initAreas(Board board) {
		SVGElement area = (SVGElement) svg.getElementById("area");
		NodeList<Element> nodeList = area.getElementsByTagName("path");
		for (int i = 0; i < nodeList.getLength(); ++i) {
			SVGElement item = (SVGElement) nodeList.getItem(i);
			OMElement node = OMElement.convert(item);
			((HasMouseMoveHandlers) node).addMouseMoveHandler(this);
			((HasMouseDownHandlers) node).addMouseDownHandler(this);
		}
	}

	@Override
	public void onMouseMove(MouseMoveEvent event) {
		if (event.isControlKeyDown()) {
			Element newSource = event.getRelativeElement();
			if (source != newSource && source != null) {
				Hex from = board.getHex(source.getId());
				Hex to = board.getHex(newSource.getId());
				drawLine(from, to);
				if (path.isEmpty()) {
					path.add(source);
				}
				path.add(newSource);
			}
			setCurrent(newSource);
		}
	}

	@Override
	public void onMouseDown(MouseDownEvent event) {
		if (event.isControlKeyDown()) {
			if(path.isEmpty()) {
				String id = event.getRelativeElement().getId();
				mark(Arrays.asList(board.getHex(id)));
			}else{
				nextSegment();
			}
		}
	}

	public void drawLine(Hex from, Hex to) {
		drawLine(getCenter(from), getCenter(to), to.getId());
	}

	public void drawLine(Position start, Position end, String id) {
		OMSVGLineElement line = doc.createSVGLineElement(start.x, start.y, end.x, end.y);
		line.setId("editline-" + id);
		line.getStyle().setSVGProperty("stroke-width", "5");
		line.getStyle().setSVGProperty("stroke", color);
		svg.getElementById("markers").appendChild(line.getElement());
	}

	public void setCurrent(Element newSource) {
		source = newSource;
		mark(Arrays.asList(board.getHex(source.getId())));
	}

	@Override
	public void onKeyPress(KeyPressEvent event) {
		char c = event.getCharCode();
		if (c == 'h' || c == '?') {
			Window.alert("Help:\n" + "q - Undo\n" + "l - load all\n" + "c - clear/reset" + "n - save current and start next segment" + "t - set color" + "o - open by id"
					+ "s - save current");
		} else if (c == 'q') {
			removeLine(path.pop().getId());
			setCurrent(path.peek());
		} else if (c == 'l') {
			loadAll();
		} else if (c == 'c') {
			clearMarkers();
		} else if (c == 'n') {
			nextSegment();
		} else if (c == 't') {
			String newName = Window.prompt("Line color", color);
			if (newName != null) {
				color = newName;
				svg.getElementById("editline-" + id).getStyle().setProperty("stroke", color);
			}
		} else if (c == 's') {
			save();
		}
	}

	private void loadAll() {
		status("Load all...");
		clearMarkers();
		EditServiceAsync service = GWT.create(EditService.class);
		service.load(new AbstractCallback<List<Map<String, String>>>() {
			@Override
			public void onSuccess(List<Map<String, String>> results) {
				for (Map<String, String> map : results) {
					color = map.get("color");
					id = Math.max(Long.parseLong(map.get("id")), id);					
					draw(map.get("src"));
				}
				status("Loaded all - "+id);
			}
		});
	}

	private void nextSegment() {
		if (path.isEmpty()) {
			return;
		}
		save();
		reset();
		id++;
		status("Saved. New id=" + id);
		mark(new ArrayList<Hex>());
	}

	private void reset() {
		source = null;
		path.clear();
	}

	private void draw(String result) {
		reset();
		String[] ids = result.split(" ");
		Hex prev = null;
		for (String id : ids) {
			Hex hex = board.getHex(id);
			if (prev != null) {
				drawLine(prev, hex);
			}
			prev = hex;
		}
	}

	private void save() {
		status("Save...");
		EditServiceAsync service = GWT.create(EditService.class);
		StringBuilder src = new StringBuilder();
		for (Element h : path) {
			src.append(h.getId()).append(" ");
		}
		service.save(id, color, src.toString(), new AbstractCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				status("Saved " + id + " in " + color);
			}
		});
	}

	private void status(String string) {
		Window.setTitle(string+" /"+new Date().getSeconds());
	}

	public void removeLine(String id) {
		svg.getElementById("editline-" + id).removeFromParent();
	}

	@Override
	public void clearMarkers() {
		super.clearMarkers();
		reset();
	}
}
