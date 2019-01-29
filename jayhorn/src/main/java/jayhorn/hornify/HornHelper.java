package jayhorn.hornify;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Verify;

import jayhorn.solver.Prover;
import jayhorn.solver.ProverExpr;
import jayhorn.solver.ProverFun;
import jayhorn.solver.ProverTupleExpr;
import jayhorn.solver.ProverTupleType;
import jayhorn.solver.ProverType;
import soottocfg.cfg.type.BoolType;
import soottocfg.cfg.type.IntType;
import soottocfg.cfg.type.ReferenceType;
import soottocfg.cfg.type.Type;
import soottocfg.cfg.type.TypeType;
import soottocfg.cfg.variable.Variable;

public class HornHelper {

	public static final int NullValue = 0;
	
	private static HornHelper hh;
	
	public static void resetInstance() {
		hh = null;
	}

	public static HornHelper hh() {
		if (null == hh) {
			hh = new HornHelper();
		}
		return hh;
	}

	private HornHelper() {
	}

	/**
	 * Creates a ProverType from a Type.
	 * TODO: not fully implemented.
	 * 
	 * @param p
	 * @param t
	 * @return
	 */
	public ProverType getProverType(Prover p, Type t) {
		if (t == IntType.instance()) {
			return p.getIntType();
		}
		if (t == BoolType.instance()) {
			return p.getBooleanType();
		}
		if (t instanceof ReferenceType) {
			ReferenceType rt = (ReferenceType) t;
			// TODO: String content model #1
//			if (rt.getClassVariable().getName().equals("java/lang/String")) {
//				return p.getStringType();
//			} else {
				final ProverType[] subTypes = new ProverType[rt.getElementTypeList().size()];
				for (int i = 0; i < rt.getElementTypeList().size(); i++) {
					subTypes[i] = getProverType(p, rt.getElementTypeList().get(i));
				}
				return p.getTupleType(subTypes);
//			}
		}
                if (t instanceof WrappedProverType)
                    return ((WrappedProverType)t).getProverType();
		if (t instanceof TypeType) {
			return p.getIntType();
		}

		throw new IllegalArgumentException("don't know what to do with " + t);
	}
	
	public ProverTupleExpr mkNullExpression(Prover p, ProverType[] types) {
		ProverExpr[] subExprs = new ProverExpr[types.length];
		for (int i = 0; i < types.length; i++) {
			if (types[i] instanceof jayhorn.solver.BoolType) {
				subExprs[i] = p.mkLiteral(false);
			} else if (types[i] instanceof jayhorn.solver.IntType) {
				subExprs[i] = p.mkLiteral(NullValue);
			} else if (types[i] instanceof ProverTupleType) {				
				subExprs[i] = mkNullExpression(p, ((ProverTupleType)types[i]).getSubTypes());
			} else {
				throw new RuntimeException("Not implemented " + types[i].getClass());
			}
		}
		return (ProverTupleExpr) p.mkTuple(subExprs);
	}

	public ProverFun genHornPredicate(Prover p, String name, List<Variable> sortedVars) {
		final List<ProverType> types = new LinkedList<ProverType>();
		for (Variable v : sortedVars) {
			types.add(getProverType(p, v.getType()));
		}
		return p.mkHornPredicate(name, types.toArray(new ProverType[types.size()]));
	}

	private int varNum = 0;

	public int newVarNum() {
		return varNum++;
	}

        /**
         * Apply a substitution to the values of a variable map. This will only
         * check for complete matches, i.e., it won't substitute any sub-expressions
         */
        public void substitute(Map<Variable, ProverExpr> varMap,
                               Map<ProverExpr, ProverExpr> subst) {
            for (Map.Entry<Variable, ProverExpr> e : varMap.entrySet())
                if (subst.containsKey(e.getValue()))
                    e.setValue(subst.get(e.getValue()));
        }

	public List<ProverExpr> findOrCreateProverVar(Prover p, List<Variable> cfgVars, Map<Variable, ProverExpr> varMap) {
		List<ProverExpr> res = new LinkedList<ProverExpr>();
		for (Variable v : cfgVars) {
			res.add(findOrCreateProverVar(p, v, varMap));
		}
		return res;
	}

	public ProverExpr findOrCreateProverVar(Prover p, Variable v, Map<Variable, ProverExpr> varMap) {
		if (!varMap.containsKey(v)) {
			varMap.put(v, createVariable(p, v));
		}
		return varMap.get(v);
	}

	public ProverExpr createVariable(Prover p, Variable v) {
		ProverType pt = getProverType(p, v.getType());
		return p.mkHornVariable(v.getName() + "_" + newVarNum(), pt);
	}

	public ProverExpr createVariable(Prover p, String prefix, Type tp) {
		return p.mkHornVariable(prefix + newVarNum(), getProverType(p, tp));
	}

	public List<Variable> setToSortedList(Set<Variable> set) {
		List<Variable> res = new LinkedList<Variable>(set);
		if (!res.isEmpty()) {
			Collections.sort(res, new Comparator<Variable>() {
				@Override
				public int compare(final Variable object1, final Variable object2) {
					return object1.getName().compareTo(object2.getName());
				}
			});
		}
		return res;
	}
}
