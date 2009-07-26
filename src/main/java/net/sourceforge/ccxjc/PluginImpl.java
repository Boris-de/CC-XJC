/*
 * Copyright (c) 2009 The CC-XJC Project. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   o Redistributions of source code must retain the above copyright
 *     notice, this  list of conditions and the following disclaimer.
 *
 *   o Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE CC-XJC PROJECT AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE CC-XJC PROJECT OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * $Id$
 */
package net.sourceforge.ccxjc;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.EnumOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import org.xml.sax.ErrorHandler;

/**
 * CC-XJC plugin implementation.
 *
 * @author <a href="mailto:cs@schulte.it">Christian Schulte</a>
 * @version $Id$
 */
public final class PluginImpl extends Plugin
{

    private static final JType[] NO_ARGS = new JType[ 0 ];

    private static final Class[] NO_PARAMS = new Class[ 0 ];

    private static final String MESSAGE_PREFIX = "CC-XJC";

    private static final String[] IMMUTABLE_NAMES =
    {
        "java.lang.Boolean",
        "java.lang.Byte",
        "java.lang.Character",
        "java.lang.Double",
        "java.lang.Float",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Short",
        "java.lang.String",
        "java.math.BigDecimal",
        "java.math.BigInteger",
        "java.util.UUID"
    };

    private boolean success;

    private Options options;

    @Override
    public String getOptionName()
    {
        return "copy-constructor";
    }

    @Override
    public String getUsage()
    {
        return this.getMessage( "usage", null );
    }

    @Override
    public boolean run( final Outline model, final Options options, final ErrorHandler errorHandler )
    {
        this.success = true;
        this.options = options;

        this.log( Level.INFO, "title", null );

        for ( ClassOutline clazz : model.getClasses() )
        {
            if ( this.getStandardConstructor( clazz ) == null )
            {
                this.log( Level.WARNING, "couldNotAddStdCtor", new Object[]
                    {
                        clazz.implClass.binaryName()
                    } );

            }

            if ( this.getCopyConstructor( clazz ) == null )
            {
                this.log( Level.WARNING, "couldNotAddCopyCtor", new Object[]
                    {
                        clazz.implClass.binaryName()
                    } );

            }
        }

        for ( ClassOutline clazz : model.getClasses() )
        {
            if ( this.getCloneMethod( clazz ) == null )
            {
                this.log( Level.WARNING, "couldNotAddMethod", new Object[]
                    {
                        "clone",
                        clazz.implClass.binaryName()
                    } );

            }
        }

        return this.success;
    }

    private ClassOutline getClassOutline( final Outline outline, final String binaryName )
    {
        for ( ClassOutline c : outline.getClasses() )
        {
            if ( c.implClass.binaryName().equals( binaryName ) )
            {
                return c;
            }
        }

        return null;
    }

    private EnumOutline getEnumOutline( final Outline outline, final String binaryName )
    {
        for ( EnumOutline e : outline.getEnums() )
        {
            if ( e.clazz.binaryName().equals( binaryName ) )
            {
                return e;
            }
        }

        return null;
    }

    private JClass getSupertype( final List<JClass> superTypes, final JClass clazz )
    {
        JClass superType = null;

        if ( clazz._extends() != null )
        {
            superType = this.getSupertype( superTypes, clazz._extends() );
        }

        if ( superType == null )
        {
            for ( JClass c : superTypes )
            {
                if ( c.binaryName().equals( clazz.binaryName() ) )
                {
                    superType = clazz;
                    break;
                }
            }
        }

        return superType;
    }

    private ClassOutline getSuperclass( final List<ClassOutline> superClasses, final ClassOutline clazz )
    {
        ClassOutline superClass = null;

        if ( clazz.getSuperClass() != null )
        {
            superClass = this.getSuperclass( superClasses, clazz.getSuperClass() );
        }

        if ( superClass == null )
        {
            for ( ClassOutline c : superClasses )
            {
                if ( c.implClass.binaryName().equals( clazz.implClass.binaryName() ) )
                {
                    superClass = clazz;
                    break;
                }
            }
        }

        return superClass;
    }

    private JMethod getPropertyGetter( final FieldOutline f )
    {
        final JDefinedClass clazz = f.parent().implClass;
        final String name = f.getPropertyInfo().getName( true );
        JMethod getter = clazz.getMethod( "get" + name, NO_ARGS );

        if ( getter == null )
        {
            getter = clazz.getMethod( "is" + name, NO_ARGS );
        }

        return getter;
    }

    private boolean isPropertyField( final ClassOutline clazz, final String fieldName )
    {
        for ( FieldOutline f : clazz.getDeclaredFields() )
        {
            if ( f.getPropertyInfo().getName( false ).equals( fieldName ) )
            {
                return true;
            }
        }

        return false;
    }

    private boolean isImmutableType( final JType type )
    {
        for ( String s : IMMUTABLE_NAMES )
        {
            if ( type.binaryName().equals( s ) )
            {
                return true;
            }
        }

        return false;
    }

    private JMethod getCloneMethod( final ClassOutline clazz )
    {
        JMethod clone = clazz.implClass.getMethod( "clone", NO_ARGS );
        if ( clone == null )
        {
            clone = this.generateCloneMethod( clazz );
        }
        else
        {
            this.log( Level.WARNING, "methodExists", new Object[]
                {
                    "clone", clazz.implClass.binaryName()
                } );

        }

        return clone;
    }

    private JMethod getStandardConstructor( final ClassOutline clazz )
    {
        JMethod ctor = clazz.implClass.getConstructor( NO_ARGS );
        if ( ctor == null )
        {
            ctor = this.generateStandardConstructor( clazz );
        }
        else
        {
            this.log( Level.WARNING, "standardCtorExists", new Object[]
                {
                    clazz.implClass.binaryName()
                } );

        }

        return ctor;
    }

    private JMethod getCopyConstructor( final ClassOutline clazz )
    {
        JMethod ctor = clazz.implClass.getConstructor( new JType[]
            {
                clazz.implClass
            } );

        if ( ctor == null )
        {
            ctor = this.generateCopyConstructor( clazz );
        }
        else
        {
            this.log( Level.WARNING, "copyCtorExists", new Object[]
                {
                    clazz.implClass.binaryName()
                } );

        }

        return ctor;
    }

    private JMethod generateCloneMethod( final ClassOutline clazz )
    {
        JMethod cloneMethod = null;

        if ( clazz.implClass.isAbstract() )
        {
            cloneMethod = clazz.implClass.method( JMod.ABSTRACT | JMod.PUBLIC, clazz.implClass, "clone" );
        }
        else
        {
            cloneMethod = clazz.implClass.method( JMod.PUBLIC, clazz.implClass, "clone" );
            cloneMethod.body().directStatement( " // " + this.getMessage( "title", null ) );
            cloneMethod.body()._return( JExpr._new( clazz.implClass ).arg( JExpr._this() ) );
        }

        cloneMethod.annotate( Override.class );
        clazz.implClass._implements( clazz.parent().getCodeModel().ref( Cloneable.class ) );
        cloneMethod.javadoc().append( "Creates and returns a copy of this object.\n" );
        cloneMethod.javadoc().addReturn().append( "A clone of this instance." );
        return cloneMethod;
    }

    private JMethod generateStandardConstructor( final ClassOutline clazz )
    {
        final JMethod ctor = clazz.implClass.constructor( JMod.PUBLIC );
        ctor.body().directStatement( " // " + this.getMessage( "title", null ) );
        ctor.body().invoke( "super" );
        ctor.javadoc().add( "Creates a new {@code " + clazz.implClass.fullName() + "} instance." );
        return ctor;
    }

    private JMethod generateCopyConstructor( final ClassOutline clazz )
    {
        final JMethod ctor = clazz.implClass.constructor( JMod.PUBLIC );
        final JVar o = ctor.param( JMod.FINAL, clazz.implClass, "o" );

        ctor.javadoc().add( "Creates a new {@code " + clazz.implClass.fullName() +
                            "} instance by copying a given instance." );

        ctor.javadoc().addParam( o ).add( "The instance to copy or {@code null}." );

        ctor.body().directStatement( " // " + this.getMessage( "title", null ) );

        if ( clazz.getSuperClass() != null )
        {
            ctor.body().invoke( "super" ).arg( o );
        }
        else
        {
            ctor.body().invoke( "super" );
        }

        final JConditional paramNotNull = ctor.body()._if( o.ne( JExpr._null() ) );
        for ( FieldOutline field : clazz.getDeclaredFields() )
        {
            this.generateCopyProperty( field, o, paramNotNull._then() );
        }

        for ( JFieldVar field : clazz.implClass.fields().values() )
        {
            if ( !this.isPropertyField( clazz, field.name() ) &&
                 ( field.mods().getValue() & JMod.STATIC ) != JMod.STATIC )
            {
                this.generateCopyField( clazz, field, o, paramNotNull._then() );
            }
        }

        return ctor;
    }

    private void generateCopyProperty( final FieldOutline field, final JVar o, final JBlock block )
    {
        final JMethod getter = this.getPropertyGetter( field );
        final JType object = field.parent().parent().getCodeModel().ref( Object.class );
        final JClass assertionErrorType = field.parent().parent().getCodeModel().ref( AssertionError.class );

        if ( getter != null )
        {
            if ( field.getPropertyInfo().isCollection() )
            {
                block.directStatement( "// Property '" + field.getPropertyInfo().getName( true ) + "'." );
                final List<CTypeInfo> refTypes = new LinkedList<CTypeInfo>(
                    field.getPropertyInfo().ref() != null ? field.getPropertyInfo().ref() : Collections.EMPTY_LIST );

                if ( !refTypes.isEmpty() )
                {
                    final List<JClass> allTypes = new LinkedList<JClass>();
                    for ( CTypeInfo t : refTypes )
                    {
                        String typeName = t.toType( field.parent().parent(), Aspect.EXPOSED ).binaryName();
                        int idx = typeName.indexOf( '<' );

                        if ( idx != -1 )
                        {
                            typeName = typeName.substring( 0, idx );
                        }

                        final JClass javaType = field.parent().parent().getCodeModel().ref( typeName );
                        if ( !allTypes.contains( javaType ) )
                        {
                            allTypes.add( javaType );
                        }
                    }

                    final JForLoop copyLoop = block._for();
                    final JVar it = copyLoop.init( field.parent().parent().getCodeModel().ref( Iterator.class ),
                                                   "it", JExpr.invoke( o, getter ).invoke( "iterator" ) );

                    copyLoop.test( JExpr.invoke( it, "hasNext" ) );

                    final JVar next = copyLoop.body().decl( object, "next", JExpr.invoke( it, "next" ) );

                    final List<ClassOutline> allClasses = new LinkedList<ClassOutline>();
                    for ( Iterator<CTypeInfo> t = refTypes.iterator(); t.hasNext(); )
                    {
                        final JType copy = t.next().toType( field.parent().parent(), Aspect.EXPOSED );
                        final ClassOutline ref = this.getClassOutline( field.parent().parent(), copy.binaryName() );

                        if ( ref != null )
                        {
                            if ( !allClasses.contains( ref ) )
                            {
                                allClasses.add( ref );
                            }

                            t.remove();
                        }
                    }

                    final List<ClassOutline> superClasses = new LinkedList<ClassOutline>();
                    for ( ClassOutline c : allClasses )
                    {
                        final ClassOutline superClass = this.getSuperclass( allClasses, c );
                        if ( !superClasses.contains( superClass ) )
                        {
                            superClasses.add( superClass );
                        }
                    }

                    Collections.sort( superClasses, new ClassOutlineComparator() );

                    for ( ClassOutline refClass : superClasses )
                    {
                        copyLoop.body().directStatement( "// Cloneable reference '" +
                                                         refClass.implClass.binaryName() + "'." );

                        final JConditional ifInstance =
                            copyLoop.body()._if( next._instanceof( refClass.implClass ) );

                        ifInstance._then().invoke( JExpr.invoke( getter ), "add" ).
                            arg( JExpr.cast( refClass.implClass, JExpr.invoke(
                            JExpr.cast( refClass.implClass, JExpr.ref( "next" ) ), "clone" ) ) );

                        ifInstance._then()._continue();
                    }

                    final List<JClass> superTypes = new LinkedList<JClass>();

                    for ( CTypeInfo type : refTypes )
                    {
                        String typeName = type.toType( field.parent().parent(), Aspect.EXPOSED ).binaryName();
                        int idx = typeName.indexOf( '<' );

                        if ( idx != -1 )
                        {
                            typeName = typeName.substring( 0, idx );
                        }

                        final JClass javaType = field.parent().parent().getCodeModel().ref( typeName );
                        final JClass superType = this.getSupertype( allTypes, javaType );
                        if ( !superTypes.contains( superType ) )
                        {
                            superTypes.add( superType );
                        }
                    }

                    Collections.sort( superTypes, new JClassComparator() );

                    for ( Iterator<JClass> t = superTypes.iterator(); t.hasNext(); )
                    {
                        final JType refType = t.next();
                        final JConditional ifInstance = copyLoop.body()._if( next._instanceof( refType ) );
                        if ( !this.isCloneable( refType ) )
                        {
                            ifInstance._then().directStatement( "// Immutable type '" + refType.binaryName() + "'." );
                            ifInstance._then().invoke( JExpr.invoke( getter ), "add" ).
                                arg( JExpr.cast( refType, next ) );

                            ifInstance._then()._continue();

                            if ( this.isImmutableType( refType ) )
                            {
                                t.remove();
                            }
                        }
                        else
                        {
                            ifInstance._then().directStatement( "// Cloneable type '" + refType.binaryName() + "'." );

                            JBlock cloneBlock = ifInstance._then();
                            if ( this.isThrowingCloneNotSupportedException( refType ) )
                            {
                                final JTryBlock tryCloneable = cloneBlock._try();
                                final JCatchBlock catchCloneNotSupported =
                                    tryCloneable._catch( field.parent().parent().getCodeModel().ref(
                                    CloneNotSupportedException.class ) );

                                catchCloneNotSupported.body()._throw( JExpr._new(
                                    field.parent().parent().getCodeModel()._ref( AssertionError.class ) ).
                                    arg( catchCloneNotSupported.param( "e" ) ) );

                                cloneBlock = tryCloneable.body();
                            }

                            cloneBlock.invoke( JExpr.invoke( getter ), "add" ).arg(
                                JExpr.cast( refType, JExpr.invoke( JExpr.cast( refType, next ), "clone" ) ) );

                            t.remove();
                        }
                    }

                    copyLoop.body()._throw( JExpr._new( assertionErrorType ).arg( JExpr.lit(
                        "Unexpected instance '" ).plus( next ).plus( JExpr.lit(
                        "' for property '" + field.getPropertyInfo().getName( true ) +
                        "' of class '" + field.parent().implClass.binaryName() + "'." ) ) ) );

                    if ( !superTypes.isEmpty() )
                    {
                        this.log( Level.WARNING, "cannotCopyProperty", new Object[]
                            {
                                field.getPropertyInfo().getName( true ),
                                field.parent().implClass.binaryName()
                            } );

                    }
                }
                else
                {
                    this.log( Level.SEVERE, "typesNotFound", new Object[]
                        {
                            field.getPropertyInfo().getName( true ),
                            field.parent().implClass.binaryName(),
                        } );

                    this.success = false;
                }
            }
            else
            {
                final ClassOutline ref =
                    this.getClassOutline( field.parent().parent(), field.getRawType().binaryName() );

                if ( ref != null )
                {
                    block.directStatement( "// Property '" + field.getPropertyInfo().getName( true ) + "'." );
                    final JConditional notnull = block._if( JExpr.invoke( o, getter ).ne( JExpr._null() ) );
                    notnull._then().directStatement( "// Cloneable reference '" +
                                                     ref.implClass.binaryName() + "'." );

                    notnull._then().assign( JExpr.refthis( field.getPropertyInfo().getName( false ) ),
                                            JExpr.cast( ref.implClass, JExpr.invoke( o, getter ).invoke( "clone" ) ) );

                    notnull._else().assign( JExpr.refthis( field.getPropertyInfo().getName( false ) ),
                                            JExpr._null() );

                }
                else
                {
                    final EnumOutline enumOutline =
                        this.getEnumOutline( field.parent().parent(), field.getRawType().binaryName() );

                    if ( field.getRawType().isPrimitive() || enumOutline != null ||
                         !this.isCloneable( field.getRawType() ) )
                    {
                        block.directStatement( "// Immutable property '" + field.getPropertyInfo().getName( true ) +
                                               "' of type '" + field.getRawType().binaryName() + "'." );

                        block.assign( JExpr.refthis( field.getPropertyInfo().getName( false ) ),
                                      JExpr.invoke( o, getter ) );

                        if ( !field.getRawType().isPrimitive() && enumOutline == null &&
                             !this.isImmutableType( field.getRawType() ) )
                        {
                            this.log( Level.WARNING, "cannotCopyProperty", new Object[]
                                {
                                    field.getPropertyInfo().getName( true ),
                                    field.parent().implClass.binaryName()
                                } );

                        }
                    }
                    else
                    {
                        block.directStatement( "// Cloneable property '" + field.getPropertyInfo().getName( true ) +
                                               "' of type '" + field.getRawType().binaryName() + "'." );

                        JConditional notnull = null;
                        if ( this.isThrowingCloneNotSupportedException( field.getRawType() ) )
                        {
                            final JTryBlock tryCloneable = block._try();
                            final JCatchBlock catchCloneNotSupported =
                                tryCloneable._catch( field.parent().parent().getCodeModel().ref(
                                CloneNotSupportedException.class ) );

                            catchCloneNotSupported.body()._throw( JExpr._new( field.parent().parent().getCodeModel().
                                _ref( AssertionError.class ) ).arg( catchCloneNotSupported.param( "e" ) ) );

                            notnull = tryCloneable.body()._if( JExpr.invoke( o, getter ).ne( JExpr._null() ) );
                        }
                        else
                        {
                            notnull = block._if( JExpr.invoke( o, getter ).ne( JExpr._null() ) );
                        }

                        notnull._then().assign( JExpr.refthis( field.getPropertyInfo().getName( false ) ),
                                                JExpr.cast( field.getRawType(),
                                                            JExpr.invoke( o, getter ).invoke( "clone" ) ) );

                        notnull._else().assign( JExpr.refthis( field.getPropertyInfo().getName( false ) ),
                                                JExpr._null() );

                    }
                }
            }
        }
        else
        {
            this.log( Level.SEVERE, "getterNotFound", new Object[]
                {
                    field.getPropertyInfo().getName( true ),
                    field.parent().implClass.binaryName(),
                } );

            this.success = false;
        }
    }

    private void generateCopyField( final ClassOutline clazz, final JFieldVar field, final JVar o, final JBlock block )
    {
        if ( !field.type().isPrimitive() && this.isCloneable( field.type() ) )
        {
            block.directStatement( "// Cloneable field '" + field.name() + "'." );

            JConditional refNotNull = null;
            if ( this.isThrowingCloneNotSupportedException( field.type() ) )
            {
                final JTryBlock tryClone = block._try();
                final JCatchBlock cloneNotSupported = tryClone._catch( clazz.parent().getCodeModel().ref(
                    CloneNotSupportedException.class ) );

                cloneNotSupported.body()._throw( JExpr._new( clazz.parent().getCodeModel().ref(
                    AssertionError.class ) ).arg( cloneNotSupported.param( "e" ) ) );

                refNotNull = tryClone.body()._if( JExpr.ref( o, field ).ne( JExpr._null() ) );
            }
            else
            {
                refNotNull = block._if( JExpr.ref( o, field ).ne( JExpr._null() ) );
            }

            refNotNull._then().assign( JExpr.refthis( field.name() ),
                                       JExpr.cast( field.type(), JExpr.ref( o, field ).invoke( "clone" ) ) );

            refNotNull._else().assign( JExpr.refthis( field.name() ), JExpr._null() );
        }
        else
        {
            block.directStatement( "// Immutable field '" + field.name() + "'." );
            block.assign( JExpr.refthis( field.name() ), JExpr.ref( o, field ) );

            if ( !field.type().isPrimitive() &&
                 !this.isImmutableType( field.type() ) &&
                 this.getEnumOutline( clazz.parent(), field.type().binaryName() ) == null )
            {
                this.log( Level.WARNING, "cannotCopyField", new Object[]
                    {
                        field.name(),
                        clazz.implClass.binaryName()
                    } );

            }
        }
    }

    private ClassLoader getClassLoader()
    {
        ClassLoader cl = this.getClass().getClassLoader();
        if ( cl == null )
        {
            cl = ClassLoader.getSystemClassLoader();
        }

        return cl;
    }

    private boolean isCloneable( final JType type )
    {
        try
        {
            return Cloneable.class.isAssignableFrom( Class.forName( type.binaryName(), false, this.getClassLoader() ) );
        }
        catch ( ClassNotFoundException e )
        {
            this.log( Level.WARNING, "classNotFound", new Object[]
                {
                    e.getMessage()
                } );

        }

        return false;
    }

    private boolean isThrowingCloneNotSupportedException( final JType type )
    {
        try
        {
            final Class clazz = Class.forName( type.binaryName(), false, this.getClassLoader() );
            final Method clone = clazz.getMethod( "clone", NO_PARAMS );
            for ( Class e : clone.getExceptionTypes() )
            {
                if ( CloneNotSupportedException.class.isAssignableFrom( e ) )
                {
                    return true;
                }
            }
        }
        catch ( NoSuchMethodException e )
        {
            this.log( Level.WARNING, "methodNotFound", new Object[]
                {
                    e.getMessage()
                } );

        }
        catch ( ClassNotFoundException e )
        {
            this.log( Level.WARNING, "classNotFound", new Object[]
                {
                    e.getMessage()
                } );

        }

        return false;
    }

    private String getMessage( final String key, final Object args )
    {
        final ResourceBundle bundle = ResourceBundle.getBundle( "net/sourceforge/ccxjc/PluginImpl" );
        return new MessageFormat( bundle.getString( key ) ).format( args );
    }

    private void log( final Level level, final String key, final Object args )
    {
        final StringBuffer b = new StringBuffer().append( "[" ).append( MESSAGE_PREFIX ).append( "] [" ).
            append( level ).append( "] " ).append( this.getMessage( key, args ) );

        if ( this.options != null && !this.options.quiet )
        {
            final int l =
                this.options != null && this.options.debugMode ? Level.ALL.intValue() : Level.INFO.intValue();

            if ( level.intValue() >= l )
            {
                if ( level.intValue() <= Level.INFO.intValue() )
                {
                    System.out.println( b.toString() );
                }
                else
                {
                    System.err.println( b.toString() );
                }
            }
        }
    }

}

class JClassComparator implements Comparator<JClass>
{

    private int recurse( final JClass o1, final JClass o2, final int hierarchy )
    {
        if ( o1.binaryName().equals( o2.binaryName() ) )
        {
            return 0 - hierarchy;
        }
        if ( o2._extends() != null )
        {
            return this.recurse( o1, o2._extends(), hierarchy + 1 );
        }

        return 0;
    }

    public int compare( final JClass o1, final JClass o2 )
    {
        return this.recurse( o1, o2, 0 );
    }

}

class ClassOutlineComparator implements Comparator<ClassOutline>
{

    private int recurse( final ClassOutline o1, final ClassOutline o2, final int hierarchy )
    {
        if ( o1.implClass.binaryName().equals( o2.implClass.binaryName() ) )
        {
            return 0 - hierarchy;
        }
        if ( o2.getSuperClass() != null )
        {
            return this.recurse( o1, o2.getSuperClass(), hierarchy + 1 );
        }

        return 0;
    }

    public int compare( final ClassOutline o1, final ClassOutline o2 )
    {
        return this.recurse( o1, o2, 0 );
    }

}
