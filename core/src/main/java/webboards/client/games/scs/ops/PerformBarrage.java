package webboards.client.games.scs.ops;

import webboards.client.data.GameCtx;
import webboards.client.data.ref.CounterId;
import webboards.client.display.VisualCoords;
import webboards.client.ex.WebBoardsException;
import webboards.client.games.Hex;
import webboards.client.games.scs.*;
import webboards.client.games.scs.bastogne.ArtyType;
import webboards.client.games.scs.bastogne.BastogneSide;
import webboards.client.ops.Operation;
import webboards.client.ops.ServerContext;
import webboards.client.ops.generic.DiceRoll;

public class PerformBarrage extends Operation {
	private static final long serialVersionUID = 1L;
	private CounterId arty;
	private Hex target;
	private boolean resultDG = false;
	private int killSteps = 0;
	private int diceRoll = -1;
	private int killRoll = -1;

	@SuppressWarnings("unused")
	private PerformBarrage() {
	}

	public boolean isResultDG() {
		return resultDG;
	}

	public PerformBarrage(SCSCounter arty, Hex target) {
		this.arty = arty.ref();
		this.target = target;
	}

	@Override
	public void drawDetails(GameCtx ctx) {
		SCSCounter attacker = (SCSCounter) ctx.board.getInfo(arty);
		SCSColor color;
		if (diceRoll < 0) {
			// before dice rolls
			color = SCSColor.DELCARE;
		} else {
			if (resultDG) {
				if (killSteps > 0)
					color = SCSColor.SUCCESS;
				else
					color = SCSColor.PARTIAL_SUCCESS;
			} else {
				color = SCSColor.FAILURE;
			}
		}
		String id = getArrowId(attacker);
		ctx.display.drawArrow(attacker.getPosition(), target, id, color.getColor());
		SCSBoard board = (SCSBoard) ctx.board;
		board.declareBarrage(attacker, target);
	}

	public static String getArrowId(SCSCounter a) {
		return "barrage_" + a.ref();
	}

	@Override
	public void serverExecute(ServerContext ctx) {
		SCSBoard board = (SCSBoard) ctx.board;
		SCSHex hex = board.getInfo(target);
		SCSCounter attacker = (SCSCounter) board.getInfo(arty);
		float attack = hex.applyBarrageModifiers(attacker.getAttack());

		DiceRoll roll = new DiceRoll();
		roll.dice = 1;
		roll.sides = 6;
		roll.serverExecute(ctx);
		diceRoll = roll.getSum();

		resultDG = (diceRoll <= attack);
		if (resultDG) {
			roll.serverExecute(ctx);
			killRoll = roll.getSum();
			int value = getKillRollValue(attacker);
            if (killRoll >= value) {
                killSteps = 1;                
            } else {
                killSteps = 0;
            }
            placeDG(board);
		} else {
			killRoll = 0;
			killSteps = 0;
		}
	}

	private SCSMarker placeDG(SCSBoard board) {
		SCSHex hex = board.getInfo(target);
		if(hex.getMarkers().isEmpty()) {
			BastogneSide tgOwner = hex.getUnits().get(0).getOwner();			
			SCSMarker dg = new SCSMarker("dg" + target,
                    "admin/misc_"+tgOwner.name().toLowerCase()+"-dg.png",
                    tgOwner);
			board.place(target, dg);
			return dg;
		}
		return null;
	}

	private int getKillRollValue(SCSCounter attacker) {
		ArtyType artyType = attacker.getArtyType();
		if(artyType == null) {
			throw new WebBoardsException(attacker.ref()+" is missing artyType");
		}
		switch (artyType) {
		case YELLOW:  return 4;
		case GUNS_88: return 5;
		case OTHER:   return 6;		
		default:
			throw new WebBoardsException("Unsupported ArtyType:"+artyType);
		}
	}

	@Override
	public void postServer(GameCtx ctx) {
		SCSBoard board = (SCSBoard) ctx.board;
        if(resultDG) {
    		SCSMarker dg = placeDG(board);
			ctx.display.createCounter(dg, ctx.board);
			ctx.display.alignStack(target);
		}
		VisualCoords pos = ctx.display.getCenter(target);
		ctx.display.clearOds(arty.toString());
        CombatResult result = new CombatResult("D"+killSteps);
        board.showingResult(target, result);
		String res = resultDG ? "DG" : "";
		if(killSteps > 0) {
			res += killSteps;
		}
		//;filter:url(#filter5698)
		ctx.display.showResults(pos, res);
	}

	@Override
	public String toString() {
		String dg = resultDG ? ("DG-" + killSteps) : "Ineffective";
		String killDice = killRoll > 0 ? ", " + killRoll : "";
		return "Barrage on " + target + " by " + arty + ": " + dg + " (" + diceRoll + killDice + ")";
	}

}
