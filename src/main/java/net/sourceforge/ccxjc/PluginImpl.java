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
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForEach;
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
import java.util.Collection;
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

    private boolean success;

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

        for ( ClassOutline clazz : model.getClasses() )
        {
            if ( this.getStandardConstructor( clazz ) == null )
            {
                this.log( Level.SEVERE, "couldNotAddStdCtor", new Object[]
                    {
                        clazz.implClass.binaryName()
                    } );

                this.success = false;
            }

            if ( this.getCopyConstructor( clazz ) == null )
            {
                this.log( Level.SEVERE, "couldNotAddCopyCtor", new Object[]
                    {
                        clazz.implClass.binaryName()
                    } );

                this.success = false;
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

    private JMethod generateStandardConstructor( final ClassOutline clazz )
    {
        final JMethod ctor = clazz.implClass.constructor( JMod.PUBLIC );
        ctor.body().invoke( "super" );
        ctor.javadoc().add( "Creates a new {@code " + clazz.implClass.fullName() + "} instance." );
        return ctor;
    }

    private JMethod generateCopyConstructor( final ClassOutline clazz )
    {
        final JMethod ctor = clazz.implClass.constructor( JMod.PUBLIC );
        final JVar o = ctor.param( clazz.implClass, "o" );

        ctor.javadoc().add( "Creates a new {@code " + clazz.implClass.fullName() +
                            "} instance by copying a given instance." );

        ctor.javadoc().addParam( o ).add( "The instance to copy or {@code null}." );

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
            if ( !this.isPropertyField( clazz, field.name() ) )
            {
                if ( !field.type().isPrimitive() && this.isCloneable( field.type() ) )
                {
                    JConditional refNotNull = null;
                    if ( this.isThrowingCloneNotSupportedException( field.type() ) )
                    {
                        final JTryBlock tryClone = paramNotNull._then()._try();
                        final JCatchBlock cloneNotSupported = tryClone._catch( clazz.parent().getCodeModel().ref(
                            CloneNotSupportedException.class ) );

                        cloneNotSupported.body()._throw( JExpr._new( clazz.parent().getCodeModel().ref(
                            AssertionError.class ) ).arg( cloneNotSupported.param( "e" ) ) );

                        refNotNull = tryClone.body()._if( JExpr.ref( o, field ).ne( JExpr._null() ) );
                    }
                    else
                    {
                        refNotNull = paramNotNull._then()._if( JExpr.ref( o, field ).ne( JExpr._null() ) );
                    }

                    refNotNull._then().assign( JExpr.refthis( field.name() ),
                                               JExpr.cast( field.type(), JExpr.ref( o, field ).invoke( "clone" ) ) );

                    refNotNull._else().assign( JExpr.refthis( field.name() ), JExpr._null() );

                    this.log( Level.WARNING, "clonesReference", new Object[]
                        {
                            field.name(),
                            clazz.implClass.binaryName()
                        } );

                }
                else
                {
                    paramNotNull._then().assign( JExpr.refthis( field.name() ), JExpr.ref( o, field ) );
                    this.log( Level.WARNING, "copiesReference", new Object[]
                        {
                            field.name(),
                            clazz.implClass.binaryName()
                        } );

                }
            }
        }

        return ctor;
    }

    private void generateCopyProperty( final FieldOutline field, final JVar o, final JBlock block )
    {
        final JMethod getter = this.getPropertyGetter( field );

        if ( getter != null )
        {
            if ( field.getPropertyInfo().isCollection() )
            {
                final Collection<? extends CTypeInfo> refTypes = field.getPropertyInfo().ref();
                if ( refTypes != null )
                {
                    if ( refTypes.size() == 1 )
                    {
                        final ClassOutline ref = this.getClassOutline(
                            field.parent().parent(), refTypes.iterator().next().
                            toType( field.parent().parent(), Aspect.EXPOSED ).binaryName() );

                        if ( ref != null )
                        {
                            final JForEach copyLoop =
                                block.forEach( ref.implClass, "c", JExpr.invoke( o, getter ) );

                            copyLoop.body().invoke( JExpr.invoke( getter ), "add" ).
                                arg( JExpr._new( ref.implClass ).arg( copyLoop.var() ) );

                        }
                        else
                        {
                            block.invoke( JExpr.invoke( getter ), "addAll" ).arg( JExpr.invoke( o, getter ) );
                        }
                    }
                    else
                    {
                        this.log( Level.SEVERE, "multipleTypes", new Object[]
                            {
                                field.getPropertyInfo().getName( true ),
                                field.parent().implClass.binaryName(),
                                refTypes.size()
                            } );

                        this.success = false;
                    }
                }
                else
                {
                    this.log( Level.SEVERE, "noTypes", new Object[]
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
                    final JConditional notnull = block._if( JExpr.invoke( o, getter ).ne( JExpr._null() ) );
                    notnull._then().assign( JExpr.refthis( field.getPropertyInfo().getName( false ) ),
                                            JExpr._new( ref.implClass ).arg( JExpr.invoke( o, getter ) ) );

                    notnull._else().assign( JExpr.refthis( field.getPropertyInfo().getName( false ) ),
                                            JExpr._null() );

                }
                else
                {
                    if ( field.getRawType().isPrimitive() ||
                         this.getEnumOutline( field.parent().parent(), field.getRawType().binaryName() ) != null ||
                         !this.isCloneable( field.getRawType() ) )
                    {
                        block.assign( JExpr.refthis( field.getPropertyInfo().getName( false ) ),
                                      JExpr.invoke( o, getter ) );

                    }
                    else
                    {
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
            this.log( Level.SEVERE, "noGetter", new Object[]
                {
                    field.getPropertyInfo().getName( true ),
                    field.parent().implClass.binaryName(),
                } );

            this.success = false;
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
        System.err.println( '[' + level.toString() + "] " + this.getMessage( key, args ) );
    }

}
