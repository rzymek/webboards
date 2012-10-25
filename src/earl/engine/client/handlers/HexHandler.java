package earl.engine.client.handlers;

import org.vectomatic.dom.svg.OMSVGDocument;
import org.vectomatic.dom.svg.OMSVGElement;
import org.vectomatic.dom.svg.OMSVGImageElement;
import org.vectomatic.dom.svg.OMSVGMatrix;
import org.vectomatic.dom.svg.OMSVGPathElement;
import org.vectomatic.dom.svg.OMSVGPoint;
import org.vectomatic.dom.svg.OMSVGRect;
import org.vectomatic.dom.svg.OMSVGSVGElement;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;

import earl.engine.client.Earl;
import earl.engine.client.EngineService;
import earl.engine.client.EngineServiceAsync;
import earl.engine.client.utils.EarlCallback;

public class HexHandler implements MouseOutHandler, MouseOverHandler, ClickHandler {

	private final Earl earl;

	public HexHandler(Earl earl) {
		this.earl = earl;
	}
	
	private OMSVGPoint getCenter(OMSVGPathElement e) {
		OMSVGRect bbox = e.getBBox();
		OMSVGSVGElement svg = e.getOwnerSVGElement();
		OMSVGPoint p = svg.createSVGPoint();
		float x = bbox.getX() + bbox.getWidth() / 2.0f;
		float y = bbox.getY() + bbox.getHeight() / 2.0f;
		p.setX(x);
		p.setY(y);
		return p;
	}

	public void moveToHex(OMSVGImageElement unit, OMSVGPathElement hex) {
		OMSVGRect bbox = unit.getBBox();
		OMSVGMatrix m = hex.getTransformToElement(unit);
		OMSVGPoint to = getCenter(hex);
		to = to.matrixTransform(m);

		float x = to.getX() - bbox.getWidth() / 2.0f;
		float y = to.getY() - bbox.getHeight() / 2.0f;
		unit.getX().getBaseVal().setValue(x);
		unit.getY().getBaseVal().setValue(y);
	}

	@Override
	public void onClick(ClickEvent event) {
		if (earl.selectedUnit == null) {
			return;
		}
		OMSVGPathElement hex = getHex(event);
		// drawMove(earl.selectedUnit, hex);
		moveToHex(earl.selectedUnit, hex);
		Earl.log(earl.selectedUnit.getId() + " ->" + hex.getId());
		// parent.menu.gwtexpEarl(earl.selectedUnit.id, e.target.id);
		EngineServiceAsync engine = GWT.create(EngineService.class);
		engine.updateLocation(earl.selectedUnit.getId(), 
				hex.getId(), 
				new EarlCallback<Void>());
		earl.deselectUnit();
	}

	protected OMSVGPathElement getHex(DomEvent<?> event) {
		return OMSVGDocument.convert(event.getRelativeElement());
	}

	@Override
	public void onMouseOver(MouseOverEvent evt) {
		setTargetOpacity(evt, 1);
	}

	@Override
	public void onMouseOut(MouseOutEvent evt) {
		setTargetOpacity(evt, 0.33);
	}

	protected void setTargetOpacity(DomEvent<?> evt, double opacity) {
		OMSVGElement hex = getHex(evt);
		hex.getStyle().setOpacity(opacity);
	}
}