package com.amazon.aws.am2.appmig.glassviewer.db;

import com.amazon.aws.am2.appmig.glassviewer.constructs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.amazon.aws.am2.appmig.glassviewer.db.IAppDiscoveryGraphDB.*;

public class QueryBuilder {

    public static final String Q_MATCH = "FOR i IN %1$s FILTER i._key == '%2$s' RETURN i._id";
    public static final String Q_CREATE_PROJECT = "INSERT { name: '%1$s'} IN '%2$s' RETURN NEW._id";
    public static final String Q_CREATE_PACKAGE = "INSERT { _key: '%1$s', name: '%2$s', fullPackageName: '%3$s'} IN '%4$s' RETURN NEW._id";
    public static final String Q_CREATE_CLASS = "INSERT { _key: '%1$s', name: '%2$s', fullClassName: '%3$s', package: '%4$s', "
            + "public: '%5$s', default: '%6$s', final: '%7$s', abstract: '%8$s', filePath: '%9$s'} IN  '%10$s' RETURN NEW._id";
    public static final String Q_UPDATE_CLASS = "  UPDATE '%1$s' WITH {name: '%2$s', fullClassName: '%3$s', package: '%4$s', "
            + "public: '%5$s', default: '%6$s', final: '%7$s', abstract: '%8$s', filePath: '%9$s'} IN  '%10$s' RETURN NEW._id";
    public static final String Q_CREATE_METHOD = "INSERT { name: '%1$s', returnType: '%2$s', classname: '%3$s', packagename: '%4$s'} IN '%5$s' RETURN NEW._id";
    public static final String Q_READ_METHOD = "FOR i IN %1$s FILTER i.name == '%2$s' && i.returnType == '%3$s' && i.classname == '%4$s' && i.packagename == '%5$s' RETURN i._id";
    public static final String Q_CREATE_IMPORT = "INSERT { _key: '%1$s', name: '%2$s', package: '%3$s', fullName: '%4$s', startAt: '%5$s'} IN '%6$s' RETURN NEW._id";
    public static final String Q_READ_IMPORT = "FOR i IN %1$s FILTER i.name == '%2$s' && i.package == '%3$s' RETURN i._id";
    public static final String Q_CREATE_CLASS_VARIABLE = "INSERT { name: '%1$s', type: '%2$s', modifiers: '%3$s', annotations: '%4$s', startsAt: '%5$s', endsAt: '%6$s', classname: '%7$s', packagename: '%8$s'} IN '%9$s' RETURN NEW._id";
    public static final String Q_READ_CLASS_VARIABLE = "FOR i in %1$s FILTER i.name == '%2$s' && i.type == '%3$s' && i.modifiers == '%4$s' && i.annotations == '%5$s' && i.startsAt == '%6$s' && i.endsAt == '%7$s' && i.classname == '%8$s' && i.packagename == '%9$s' RETURN i._id";
    public static final String Q_CREATE_REL = "UPSERT {_from: '%1$s', _to: '%2$s'} INSERT { _from: '%1$s', _to: '%2$s' } UPDATE {_from: '%1$s', _to: '%2$s'} IN '%3$s'";
    public static final String Q_FETCH_MATCHING_CLASS_IMPORT = "WITH %1$s, %2$s FOR vertex, path IN 1..1 OUTBOUND '%3$s' GRAPH '%4$s' FILTER vertex.fullName LIKE '%5$s' return vertex.name";
    public static final String Q_FETCH_MATCHING_CLASS_VARIABLE_IMPORT = "WITH %1$s, %2$s FOR vertex, path IN 1..1 OUTBOUND '%3$s' GRAPH '%4$s' FILTER vertex.type IN %5$s return vertex";
    public static final String CLASS_TYPE = "Class";
    public static final String TRUE = "true";
    public static final String FALSE = "false";
    private final static Logger LOGGER = LoggerFactory.getLogger(QueryBuilder.class);
    
    public static String buildProjectNode(ProjectConstruct pc, OP operation) {
    	String query = null;
    	if (operation == OP.CREATE) {
    		query = String.format(Q_CREATE_PROJECT, pc.getName(), PROJECT_COLLECTION);
    	} else if (operation == OP.READ) {
            query = String.format(Q_MATCH, PROJECT_COLLECTION, pc.getName());
        }
    	LOGGER.debug("query buildProjectNode is:{}", query);
    	return query;
    }

    public static String buildPackageNode(PackageConstruct pc, OP operation) {
        String query = null;
        if (operation == OP.CREATE) {
            query = String.format(Q_CREATE_PACKAGE, pc.getFullPackageName(), pc.getPackageName(), pc.getFullPackageName(), PACKAGE_COLLECTION);
        } else if (operation == OP.READ) {
            query = String.format(Q_MATCH, PACKAGE_COLLECTION, pc.getFullPackageName());
        }
        LOGGER.debug("query buildPackageNode is:{}", query);
        return query;
    }

    private static String buildClassNode(ClassConstruct cc) {
        String query = String.format(Q_CREATE_CLASS, cc.getFullClassName(), cc.getName(), cc.getFullClassName(), cc.getPackageName(),
                cc.isPublic() ? TRUE : FALSE, cc.isDefault() ? TRUE : FALSE, cc.isFinal() ? TRUE : FALSE,
                cc.isAbstract() ? TRUE : FALSE, cc.getAbsoluteFilePath(), CLASS_COLLECTION);
        LOGGER.debug("query buildClassNode is: {}", query);
        return query;
    }

    private static String buildInterfaceNode(InterfaceConstruct ic) {
        String query = String.format(Q_CREATE_CLASS, ic.getFullClassName(), ic.getName(), ic.getFullClassName(), ic.getPackageName(),
                ic.isPublic() ? TRUE : FALSE, ic.isDefault() ? TRUE : FALSE, FALSE, TRUE, ic.getAbsoluteFilePath(), CLASS_COLLECTION);
        LOGGER.debug("query buildInterfaceNode is: {}", query);
        return query;
    }

    public static String getMatchingClassImport(ClassConstruct cc, String importStmt) throws Exception {
        try {
            String query = String.format(Q_FETCH_MATCHING_CLASS_IMPORT, CLASS_COLLECTION, IMPORT_COLLECTION, CLASS_COLLECTION + "/" + cc.getFullClassName(), GRAPH_NAME, importStmt + "%");
            LOGGER.debug("query getMatchingClassImport is:{}", query);
            return query;
        } catch (StringIndexOutOfBoundsException e) {
            throw new Exception("Invalid Import Statement: " + importStmt);
        }
    }

    public static String getMatchingClassImport(InterfaceConstruct ic, String importStmt) throws Exception {
        try {
            String query = String.format(Q_FETCH_MATCHING_CLASS_IMPORT, CLASS_COLLECTION, IMPORT_COLLECTION, CLASS_COLLECTION + "/" + ic.getFullClassName(), GRAPH_NAME, importStmt + "%");
            LOGGER.debug("query getMatchingClassImport is:{}", query);
            return query;
        } catch (StringIndexOutOfBoundsException e) {
            throw new Exception("Invalid Import Statement: " + importStmt);
        }
    }

    public static String getMatchingClassVariableImport(ClassConstruct cc, List<String> matchingImports) {
        List<String> filter = matchingImports.stream().map(importName -> "'" + importName + "'").collect(Collectors.toList());
        String query = String.format(Q_FETCH_MATCHING_CLASS_VARIABLE_IMPORT, CLASS_COLLECTION, VARIABLE_COLLECTION, CLASS_COLLECTION + "/" + cc.getFullClassName(), GRAPH_NAME, filter);
        LOGGER.debug("query getMatchingClassVariableImport is:{}", query);
        return query;
    }

    public static String getMatchingClassVariableImport(InterfaceConstruct ic, List<String> matchingImports) {
        List<String> filter = matchingImports.stream().map(importName -> "'" + importName + "'").collect(Collectors.toList());
        String query = String.format(Q_FETCH_MATCHING_CLASS_VARIABLE_IMPORT, CLASS_COLLECTION, VARIABLE_COLLECTION, CLASS_COLLECTION + "/" + ic.getFullClassName(), GRAPH_NAME, filter);
        LOGGER.debug("query getMatchingClassVariableImport is:{}", query);
        return query;
    }

    public static String buildImportNode(ImportConstruct ic, OP operation) {
        String query = null;
        if (operation == OP.CREATE) {
            query = String.format(Q_CREATE_IMPORT, ic.getPackageName() + "." + ic.getClassName(), ic.getClassName(), ic.getPackageName(), ic.getPackageName() + "." + ic.getClassName(), ic.getStartAt(), IMPORT_COLLECTION);
        } else if (operation == OP.READ) {
            query = String.format(Q_READ_IMPORT, IMPORT_COLLECTION, ic.getClassName(), ic.getPackageName());
        }
        LOGGER.debug("query buildImportNode is:{}", query);
        return query;
    }

    public static String buildMethodNode(ClassConstruct cc, MethodConstruct mc, QueryBuilder.OP operation) {
        String query = null;
        if (operation == QueryBuilder.OP.CREATE) {
            query = String.format(Q_CREATE_METHOD, mc.getName(), mc.getReturnType(), cc.getFullClassName(), cc.getPackageName(), METHOD_COLLECTION);
        } else if (operation == QueryBuilder.OP.READ) {
            query = String.format(Q_READ_METHOD, METHOD_COLLECTION, mc.getName(), mc.getReturnType(), cc.getFullClassName(), cc.getPackageName());
        }
        LOGGER.debug("query buildMethodNode is: {}", query);
        return query;
    }

    public static String buildMethodNode(InterfaceConstruct ic, MethodConstruct mc, OP operation) {
        String query = null;
        if (operation == OP.CREATE) {
            query = String.format(Q_CREATE_METHOD, mc.getName(), mc.getReturnType(), ic.getFullClassName(), ic.getPackageName(), METHOD_COLLECTION);
        } else if (operation == OP.READ) {
            query = String.format(Q_READ_METHOD, METHOD_COLLECTION, mc.getName(), mc.getReturnType(), ic.getFullClassName(), ic.getPackageName());
        }
        LOGGER.debug("query buildMethodNode is: {}", query);
        return query;
    }

    public static String buildClassVariableNode(ClassConstruct cc, ClassVariableConstruct cvc, OP operation) {
        String query = null;
        if (operation == OP.CREATE) {
            query = String.format(Q_CREATE_CLASS_VARIABLE, cvc.getName(), cvc.getVariableType(), cvc.getVariableModifiers(), cvc.getVariableAnnotations(), cvc.getMetaData().getStartsAt(),
                    cvc.getMetaData().getEndsAt(), cc.getFullClassName(), cc.getPackageName(), VARIABLE_COLLECTION);
        } else if (operation == OP.READ) {
            query = String.format(Q_READ_CLASS_VARIABLE, VARIABLE_COLLECTION, cvc.getName(), cvc.getVariableType(), cvc.getVariableModifiers(), cvc.getVariableAnnotations(), cvc.getMetaData().getStartsAt(),
                    cvc.getMetaData().getEndsAt(), cc.getFullClassName(), cc.getPackageName());
        }
        LOGGER.debug("query buildClassVariableNode is {}", query);
        return query;
    }

    public static String buildClassVariableNode(InterfaceConstruct ic, ClassVariableConstruct cvc, OP operation) {
        String query = null;
        if (operation == OP.CREATE) {
            query = String.format(Q_CREATE_CLASS_VARIABLE, cvc.getName(), cvc.getVariableType(), cvc.getVariableModifiers(), cvc.getVariableAnnotations(), cvc.getMetaData().getStartsAt(),
                    cvc.getMetaData().getEndsAt(), ic.getFullClassName(), ic.getPackageName(), VARIABLE_COLLECTION);
        } else if (operation == OP.READ) {
            query = String.format(Q_READ_CLASS_VARIABLE, VARIABLE_COLLECTION, cvc.getName(), cvc.getVariableType(), cvc.getVariableModifiers(), cvc.getVariableAnnotations(), cvc.getMetaData().getStartsAt(),
                    cvc.getMetaData().getEndsAt(), ic.getFullClassName(), ic.getPackageName());
        }
        LOGGER.debug("query buildClassVariableNode is {}", query);
        return query;
    }

    public static String buildRelation(String edge, String fromId, String toId) {
        return String.format(Q_CREATE_REL, fromId, toId, edge);
    }

    private static String updateClassNode(ClassConstruct cc) {
        String query = String.format(Q_UPDATE_CLASS, cc.getFullClassName(), cc.getName(), cc.getFullClassName(), cc.getPackageName(),
                cc.isPublic() ? TRUE : FALSE, cc.isDefault() ? TRUE : FALSE, cc.isFinal() ? TRUE : FALSE,
                cc.isAbstract() ? TRUE : FALSE, cc.getAbsoluteFilePath(), CLASS_COLLECTION);
        LOGGER.debug("query updateClassNode is: {}", query);
        return query;
    }

    public static String deleteClassNode(ClassConstruct cc) {
        return null;
    }

    private static String updateInterfaceNode(InterfaceConstruct ic) {
        String query = String.format(Q_UPDATE_CLASS, ic.getFullClassName(), ic.getName(), ic.getFullClassName(), ic.getPackageName(),
                ic.isPublic() ? TRUE : FALSE, ic.isDefault() ? TRUE : FALSE, FALSE, TRUE, ic.getAbsoluteFilePath(), CLASS_COLLECTION);
        LOGGER.debug("query updateInterfaceNode is: {}", query);
        return query;
    }

    public static String deleteInterfaceNode(InterfaceConstruct cc) {
        return null;
    }

    public static String buildQuery(JavaConstruct construct, OP operation) {
        String query = null;
        if (construct instanceof ClassConstruct) {
            ClassConstruct classConstruct = (ClassConstruct) construct;
            if (operation == OP.CREATE) {
                query = buildClassNode(classConstruct);
            } else if (operation == OP.UPDATE) {
                query = updateClassNode(classConstruct);
            } else if (operation == OP.DELETE) {
                query = deleteClassNode(classConstruct);
            }
        }
        if (construct instanceof InterfaceConstruct) {
            InterfaceConstruct interfaceConstruct = (InterfaceConstruct) construct;
            if (operation == OP.CREATE) {
                query = buildInterfaceNode(interfaceConstruct);
            } else if (operation == OP.UPDATE) {
                query = updateInterfaceNode(interfaceConstruct);
            } else if (operation == OP.DELETE) {
                query = deleteInterfaceNode(interfaceConstruct);
            }
        }

        LOGGER.debug("query buildQuery is:{}", query);
        return query;
    }

    public enum OP {
        CREATE, READ, UPDATE, DELETE
    }

    public static class MatchBuilder {

        private String type;
        private List<String> where;

        public MatchBuilder type(String type) {
            this.type = type;
            return this;
        }

        public MatchBuilder where(String condition) {
            if (this.where == null) {
                this.where = new ArrayList<>();
            }
            this.where.add(condition);
            return this;
        }

        public String build() {
            return String.format(Q_MATCH, type, where.get(0));
        }
    }
}
