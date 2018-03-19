package com.bluesgao.ghostlog.processor;

import com.bluesgao.ghostlog.annotation.GhostLog;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("com.bluesgao.ghostlog.annotation.GhostLog")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class GhostLogProcessor extends AbstractProcessor {

    //用来在编译期打log用的
    private Messager messager;
    //提供了待处理的抽象语法树
    private JavacTrees javacTrees;
    //封装了创建AST节点的一些方法
    private TreeMaker treeMaker;
    //提供了创建标识符的方法
    private Names names;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        //初始化变量
        this.messager = processingEnv.getMessager();
        this.javacTrees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //用roundEnv的getElementsAnnotatedWith方法过滤出被GhostLog这个注解标记的类，并存入set
        Set<? extends Element> set = roundEnv.getElementsAnnotatedWith(GhostLog.class);
        //遍历这个set里的每一个元素，并生成jCTree这个语法树
        for (Element element : set) {
            JCTree jcTree = javacTrees.getTree(element);
            //创建一个TreeTranslator，并重写其中的visitClassDef方法，这个方法处理遍历语法树得到的类定义部分jcClassDecl
            jcTree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    //遍历jcTree的所有成员(包括成员变量和成员函数和构造函数)
                    List<JCTree.JCVariableDecl> jcVariableDeclList = List.nil();
                    List<JCTree.JCMethodDecl> jcMethodDeclList = List.nil();

                    for (JCTree tree : jcClassDecl.defs) {
                        //变量
                        if (tree.getKind().equals(Tree.Kind.VARIABLE)) {
                            JCTree.JCVariableDecl jcVariableDecl = (JCTree.JCVariableDecl) tree;
                            jcVariableDeclList = jcVariableDeclList.append(jcVariableDecl);
                            System.out.println("--jcVariableDecl:" + jcVariableDecl.toString());
                        }
                        //方法
                        if (tree.getKind().equals(Tree.Kind.METHOD)) {
                            JCTree.JCMethodDecl jcMethodDecl = (JCTree.JCMethodDecl) tree;
                            jcMethodDeclList = jcMethodDeclList.append(jcMethodDecl);
                            System.out.println("--jcMethodDecl:" + jcMethodDecl.toString());
                        }
                    }
                    //将jcVariableDeclList的所有变量转换成需要添加的getter方法，并添加进jcClassDecl的成员中
                    for (JCTree.JCVariableDecl jcVariableDecl : jcVariableDeclList) {
                        System.out.println("--jcVariableDecl11111:" + jcClassDecl.toString());
                        //messager.printMessage(Diagnostic.Kind.NOTE, jcVariableDecl.getName() + " has been processed");
                        //jcClassDecl.defs = jcClassDecl.defs.prepend(makeGetterMethodDecl(jcVariableDecl));
                    }
                    //调用默认的遍历方法遍历处理后的jcClassDecl
                    super.visitClassDef(jcClassDecl);
                }
            });
        }
        return true;
    }

    //读取变量的定义，并创建对应的Getter方法，并试图用驼峰命名法
    private JCTree.JCMethodDecl makeGetterMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        ListBuffer<JCTree.JCStatement> statements = new ListBuffer<JCTree.JCStatement>();
        statements.append(treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")), jcVariableDecl.getName())));
        JCTree.JCBlock body = treeMaker.Block(0, statements.toList());
        //return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getNewMethodName(jcVariableDecl.getName()), jcVariableDecl.vartype, List.nil(), List.nil(), List.nil(), body, null);
        return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC), getNewMethodName(jcVariableDecl.getName()), jcVariableDecl.vartype, null, null, null, body, null);
    }

    private Name getNewMethodName(Name name) {
        String s = name.toString();
        return names.fromString("get" + s.substring(0, 1).toUpperCase() + s.substring(1, name.length()));
    }
}
