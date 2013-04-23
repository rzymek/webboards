package webboards.client;

import java.util.ArrayList;
import java.util.List;

import webboards.client.data.GameCtx;
import webboards.client.ex.ConcurrentOpException;
import webboards.client.ops.Operation;
import webboards.client.remote.ServerEngine;
import webboards.client.remote.ServerEngineAsync;
import webboards.client.utils.AbstractCallback;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Window;

public class ClientOpRunner extends AbstractCallback<Operation> implements OpRunner {
	public final static ServerEngineAsync service = GWT.create(ServerEngine.class);
	private final GameCtx ctx;
	
	public ClientOpRunner(GameCtx ctx) {
		super();
		this.ctx = ctx;
	}

	public void process(Operation op) {
		System.out.println("ClientOpRunner.service:"+service);
		if (op == null || ctx.isHistoryMode()) {
			return;
		}
		preServerExec(op);
		queue.add(op);
		processQueued();
	}

	private void preServerExec(Operation op) {
		op.updateBoard(ctx.board);
		op.draw(ctx);
	}
	
	private void postServerExec(Operation result) {
		ctx.ops.add(result);
		ctx.setPosition(ctx.ops.size()-1);
		result.postServer(ctx);
		result.drawDetails(ctx);
		ClientEngine.log("" + result);		
	}
	
	public void apply(Operation op) {
		preServerExec(op);
		postServerExec(op);
	}
	
	private final List<Operation> queue = new ArrayList<Operation>();
	private boolean processing = false;
	private Operation currentOp;
	
	/** see: https://gist.github.com/chumpy/1696249 */
	private void processQueued() {
		if (processing || queue.isEmpty()) {
			return;
		}
		processing = true;
		currentOp = queue.remove(queue.size() - 1);
		callServer();
	}

	private void callServer() {
		currentOp.index = ctx.ops.size();	
		service.process(currentOp, this);
	}
	
	@Override
	public void onSuccess(Operation result) {
		try {
			processing = false;
			postServerExec(result);
		} finally {
			processQueued();
		}
	}
	
	@Override
	public void onFailure(Throwable e) {
		if(e instanceof ConcurrentOpException) {
			ConcurrentOpException coe = (ConcurrentOpException) e;
			String msg = "Your opponent managed to perform an operation ("+coe.op+") before your's ("+currentOp+")." +
					"Do you want to resend your operation?";
			boolean resend = ask(msg);
			if(resend) {
				callServer();
			}else{
				ClientEngine.reload(ctx);
			}
		}else{
			super.onFailure(e);
		}
	}

	protected boolean ask(String msg) {
		return Window.confirm(msg);
	}

}
