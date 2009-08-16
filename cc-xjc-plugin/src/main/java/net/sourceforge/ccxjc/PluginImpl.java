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
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.BadCommandLineException;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.model.CArrayInfo;
import com.sun.tools.xjc.model.CBuiltinLeafInfo;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.model.CEnumLeafInfo;
import com.sun.tools.xjc.model.CNonElement;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.model.CWildcardTypeInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

    private static final String MESSAGE_PREFIX = "CC-XJC";

    private static final String NO_CLASSES_FIELDNAME = "CCXJC_NO_CLASSES";

    private static final String NO_OBJECTS_FIELDNAME = "CCXJC_NO_OBJECTS";

    private static final String OPTION_NAME = "copy-constructor";

    private static final String VISIBILITY_OPTION_NAME = "-cc-visibility";

    private static final String[] IMMUTABLE_NAMES =
    {
        "java.lang.Boolean",
        "java.lang.Byte",
        "java.lang.Character",
        "java.lang.Double",
        "java.lang.Enum",
        "java.lang.Float",
        "java.lang.Integer",
        "java.lang.Long",
        "java.lang.Short",
        "java.lang.String",
        "java.math.BigDecimal",
        "java.math.BigInteger",
        "java.util.UUID"
    };

    private static final String[] VISIBILITY_ARGUMENTS =
    {
        "private", "package", "protected", "public"
    };

    private boolean success;

    private Options options;

    private String visibility = "package";

    private BigInteger methodCount;

    private BigInteger constructorCount;

    private BigInteger expressionCount;

    @Override
    public String getOptionName()
    {
        return OPTION_NAME;
    }

    @Override
    public String getUsage()
    {
        return this.getMessage( "usage", null );
    }

    @Override
    public int parseArgument( final Options opt, final String[] args, final int i )
        throws BadCommandLineException, IOException
    {
        final StringBuffer supportedArguments = new StringBuffer().append( '[' );
        for ( Iterator<String> it = Arrays.asList( VISIBILITY_ARGUMENTS ).iterator(); it.hasNext(); )
        {
            supportedArguments.append( it.next() );
            if ( it.hasNext() )
            {
                supportedArguments.append( ", " );
            }
        }

        if ( args[i].startsWith( VISIBILITY_OPTION_NAME ) )
        {
            if ( i + 1 >= args.length )
            {
                throw new BadCommandLineException( this.getMessage( "badVisibilityOption", new Object[]
                    {
                        VISIBILITY_OPTION_NAME, supportedArguments.append( ']' ).toString()
                    } ) );

            }

            this.visibility = args[i + 1].trim();

            boolean supported = false;
            for ( String argument : VISIBILITY_ARGUMENTS )
            {
                if ( argument.equals( this.visibility ) )
                {
                    supported = true;
                    break;
                }
            }

            if ( !supported )
            {
                throw new BadCommandLineException( this.getMessage( "badVisibilityOption", new Object[]
                    {
                        VISIBILITY_OPTION_NAME, supportedArguments.append( ']' ).toString()
                    } ) );

            }

            return 2;
        }

        return 0;
    }

    @Override
    public boolean run( final Outline model, final Options options, final ErrorHandler errorHandler )
    {
        this.success = true;
        this.options = options;
        this.methodCount = BigInteger.ZERO;
        this.constructorCount = BigInteger.ZERO;
        this.expressionCount = BigInteger.ZERO;

        this.log( Level.INFO, "title", null );
        this.log( Level.INFO, "visibilityReport", new Object[]
            {
                this.visibility
            } );

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

            if ( this.getCloneMethod( clazz ) == null )
            {
                this.log( Level.WARNING, "couldNotAddMethod", new Object[]
                    {
                        "clone",
                        clazz.implClass.binaryName()
                    } );

            }
        }

        this.log( Level.INFO, "report", new Object[]
            {
                this.methodCount, this.constructorCount, this.expressionCount
            } );

        return this.success;
    }

    private int getVisibilityModifier()
    {
        if ( "private".equals( this.visibility ) )
        {
            return JMod.PRIVATE;
        }
        else if ( "protected".equals( this.visibility ) )
        {
            return JMod.PROTECTED;
        }
        else if ( "public".equals( this.visibility ) )
        {
            return JMod.PUBLIC;
        }

        return JMod.NONE;
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

    private FieldOutline getFieldOutline( final ClassOutline clazz, final String fieldName )
    {
        for ( FieldOutline f : clazz.getDeclaredFields() )
        {
            if ( f.getPropertyInfo().getName( false ).equals( fieldName ) )
            {
                return f;
            }
        }

        return null;
    }

    private JFieldVar getNoClassesField( final ClassOutline clazz )
    {
        JFieldVar noClassesField;
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            noClassesField = clazz._package().objectFactory().fields().get( NO_CLASSES_FIELDNAME );
            if ( noClassesField == null )
            {
                noClassesField = clazz._package().objectFactory().field(
                    JMod.PRIVATE | JMod.STATIC | JMod.FINAL, Class[].class, NO_CLASSES_FIELDNAME,
                    JExpr.newArray( clazz.parent().getCodeModel().ref( Class.class ), 0 ) );

            }
        }
        else
        {
            noClassesField = clazz.implClass.fields().get( NO_CLASSES_FIELDNAME );
            if ( noClassesField == null )
            {
                noClassesField = clazz.implClass.field(
                    JMod.PRIVATE | JMod.STATIC | JMod.FINAL, Class[].class, NO_CLASSES_FIELDNAME,
                    JExpr.newArray( clazz.parent().getCodeModel().ref( Class.class ), 0 ) );

            }
        }

        return noClassesField;
    }

    private JFieldVar getNoObjectsField( final ClassOutline clazz )
    {
        JFieldVar noObjectsField;
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            noObjectsField = clazz._package().objectFactory().fields().get( NO_OBJECTS_FIELDNAME );
            if ( noObjectsField == null )
            {
                noObjectsField = clazz._package().objectFactory().field(
                    JMod.PRIVATE | JMod.STATIC | JMod.FINAL, Object[].class, NO_OBJECTS_FIELDNAME,
                    JExpr.newArray( clazz.parent().getCodeModel().ref( Class.class ), 0 ) );

            }
        }
        else
        {
            noObjectsField = clazz.implClass.fields().get( NO_OBJECTS_FIELDNAME );
            if ( noObjectsField == null )
            {
                noObjectsField = clazz.implClass.field(
                    JMod.PRIVATE | JMod.STATIC | JMod.FINAL, Object[].class, NO_OBJECTS_FIELDNAME,
                    JExpr.newArray( clazz.parent().getCodeModel().ref( Class.class ), 0 ) );

            }
        }

        return noObjectsField;
    }

    private JInvocation getIsImmutableObjectInvocation( final ClassOutline clazz )
    {
        final int mod = this.getVisibilityModifier();
        final String methodName = "isImmutableObject";

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, boolean.class, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, boolean.class, methodName ) );

        final JVar o = m.param( JMod.FINAL, Object.class, "o" );

        m.javadoc().append( "Tests a given object against a list of known immutable types." );
        m.javadoc().addParam( o ).append( "The object to test." );
        m.javadoc().addReturn().append( "{@code true} if {@code o} is a known immutable type; {@code false} else." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        for ( String immutableName : IMMUTABLE_NAMES )
        {
            m.body()._if( o._instanceof( clazz.parent().getCodeModel().ref( immutableName ) ) ).
                _then()._return( JExpr.TRUE );

        }

        m.body()._return( JExpr.FALSE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfJaxbElementInvocation( final ClassOutline clazz )
    {
        final JClass jaxbElement = clazz.parent().getCodeModel().ref( JAXBElement.class );
        final JType[] signature =
        {
            jaxbElement
        };

        final String methodName = "copyOFJAXBElement";
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, JAXBElement.class, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, JAXBElement.class, methodName ) );

        final JVar element = m.param( JMod.FINAL, jaxbElement, "element" );

        m.javadoc().append( "Creates and returns a copy of a given {@code JAXBElement} instance." );
        m.javadoc().addParam( element ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append(
            "A copy of {@code element} or {@code null} if {@code element} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional isNotNull = m.body()._if( element.ne( JExpr._null() ) );
        final JExpression newElement = JExpr._new( jaxbElement ).
            arg( JExpr.invoke( element, "getName" ) ).
            arg( JExpr.invoke( element, "getDeclaredType" ) ).
            arg( JExpr.invoke( element, "getScope" ) ).
            arg( JExpr.invoke( element, "getValue" ) );

        final JVar copy = isNotNull._then().decl( JMod.FINAL, jaxbElement, "copy", newElement );
        isNotNull._then().add( copy.invoke( "setNil" ).arg( element.invoke( "isNil" ) ) );
        isNotNull._then().add( copy.invoke( "setValue" ).arg( this.getCopyOfObjectInvocation( clazz ).
            arg( JExpr.invoke( copy, "getValue" ) ) ) );

        isNotNull._then()._return( copy );
        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfDomElementInvocation( final ClassOutline clazz )
    {
        final JCodeModel codeModel = clazz.parent().getCodeModel();
        final JClass elementClass = codeModel.ref( Element.class );
        final JClass transformerFactory = codeModel.ref( TransformerFactory.class );
        final JClass transformerFactoryConfError = codeModel.ref( TransformerFactoryConfigurationError.class );
        final JClass transformerConfException = codeModel.ref( TransformerConfigurationException.class );
        final JClass transformerException = codeModel.ref( TransformerException.class );
        final JClass domSource = codeModel.ref( DOMSource.class );
        final JClass domResult = codeModel.ref( DOMResult.class );
        final JClass document = codeModel.ref( Document.class );
        final JClass assertionError = clazz.parent().getCodeModel().ref( AssertionError.class );
        final JType[] signature =
        {
            elementClass
        };

        final String methodName = "copyOfDOMElement";
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, elementClass, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, elementClass, methodName ) );

        final JVar element = m.param( JMod.FINAL, elementClass, "element" );

        m.javadoc().append( "Creates and returns a copy of a given DOM {@code Element} instance." );
        m.javadoc().addParam( element ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append(
            "A copy of {@code element} or {@code null} if {@code element} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional ifNotNull = m.body()._if( element.ne( JExpr._null() ) );
        final JTryBlock tryTransform = ifNotNull._then()._try();

        final JVar domSrc = tryTransform.body().decl( JMod.FINAL, domSource, "domSource",
                                                      JExpr._new( domSource ).arg( element ) );

        final JVar domRes = tryTransform.body().decl( JMod.FINAL, domResult, "domResult",
                                                      JExpr._new( domResult ) );

        tryTransform.body().add( JExpr.invoke( JExpr.invoke(
            transformerFactory.staticInvoke( "newInstance" ), "newTransformer" ), "transform" ).
            arg( domSrc ).arg( domRes ) );

        tryTransform.body()._return( JExpr.invoke( JExpr.cast( document, JExpr.invoke(
            domRes, "getNode" ) ), "getDocumentElement" ) );

        final JCatchBlock catchTransformerFactoryConfError = tryTransform._catch( transformerFactoryConfError );
        catchTransformerFactoryConfError.body()._throw( JExpr._new( assertionError ).arg(
            catchTransformerFactoryConfError.param( "e" ) ) );

        final JCatchBlock catchTransformerConfException = tryTransform._catch( transformerConfException );
        catchTransformerConfException.body()._throw( JExpr._new( assertionError ).arg(
            catchTransformerConfException.param( "e" ) ) );

        final JCatchBlock catchTransformerException = tryTransform._catch( transformerException );
        catchTransformerException.body()._throw( JExpr._new( assertionError ).arg(
            catchTransformerException.param( "e" ) ) );

        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfDurationInvocation( final ClassOutline clazz )
    {
        final JClass durationClass = clazz.parent().getCodeModel().ref( Duration.class );
        final JClass datatypeFactory = clazz.parent().getCodeModel().ref( DatatypeFactory.class );
        final JClass assertionError = clazz.parent().getCodeModel().ref( AssertionError.class );
        final JClass datatypeConfigurationException =
            clazz.parent().getCodeModel().ref( DatatypeConfigurationException.class );

        final JType[] signature =
        {
            durationClass
        };

        final String methodName = "copyOfDuration";
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, Duration.class, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, Duration.class, methodName ) );

        final JVar duration = m.param( JMod.FINAL, durationClass, "duration" );

        m.javadoc().append( "Creates and returns a copy of a given {@code Duration} instance." );
        m.javadoc().addParam( duration ).append( "The instance to copy to {@code null}." );
        m.javadoc().addReturn().append(
            "A copy of {@code duration} or {@code null} if {@code duration} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional ifNotNull = m.body()._if( duration.ne( JExpr._null() ) );
        final JTryBlock tryCopyDuration = ifNotNull._then()._try();

        tryCopyDuration.body()._return( JExpr.invoke( datatypeFactory.staticInvoke( "newInstance" ), "newDuration" ).
            arg( JExpr.invoke( duration, "toString" ) ) );

        final JCatchBlock catchDatatypeConfiguration = tryCopyDuration._catch( datatypeConfigurationException );
        catchDatatypeConfiguration.body()._throw( JExpr._new( assertionError ).arg(
            catchDatatypeConfiguration.param( "e" ) ) );

        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfQNameInvocation( final ClassOutline clazz )
    {
        final JClass qnameClass = clazz.parent().getCodeModel().ref( QName.class );
        final JType[] signature =
        {
            qnameClass
        };

        final String methodName = "copyOfQName";
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, QName.class, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, QName.class, methodName ) );

        final JVar qName = m.param( JMod.FINAL, qnameClass, "qName" );

        m.javadoc().append( "Creates and returns a copy of a given {@code QName} instance." );
        m.javadoc().addParam( qName ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append( "A copy of {@code qName} or {@code null} if {@code qName} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional ifNotNull = m.body()._if( qName.ne( JExpr._null() ) );
        ifNotNull._then()._return( JExpr._new( qnameClass ).
            arg( JExpr.invoke( qName, "getNamespaceURI" ) ).
            arg( JExpr.invoke( qName, "getLocalPart" ) ).
            arg( JExpr.invoke( qName, "getPrefix" ) ) );

        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfXMLGregorianCalendarInvocation( final ClassOutline clazz )
    {
        final JClass xmlGregorianCalendar = clazz.parent().getCodeModel().ref( XMLGregorianCalendar.class );
        final JType[] signature =
        {
            xmlGregorianCalendar
        };

        final String methodName = "copyOfXMLGregorianCalendar";
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, XMLGregorianCalendar.class, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, XMLGregorianCalendar.class, methodName ) );

        final JVar cal = m.param( JMod.FINAL, xmlGregorianCalendar, "calendar" );

        m.javadoc().append( "Creates and returns a copy of a given {@code XMLGregorianCalendar} instance." );
        m.javadoc().addParam( cal ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append(
            "A copy of {@code calendar} or {@code null} if {@code calendar} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional ifNotNull = m.body()._if( cal.ne( JExpr._null() ) );
        ifNotNull._then()._return( JExpr.cast( xmlGregorianCalendar, cal.invoke( "clone" ) ) );
        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfArrayInvocation( final ClassOutline clazz )
    {
        final JClass object = clazz.parent().getCodeModel().ref( Object.class );
        final JClass array = clazz.parent().getCodeModel().ref( Array.class );
        final JType[] signature =
        {
            object
        };

        final String methodName = "copyOfArray";
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, object, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, object, methodName ) );

        final JVar arrayArg = m.param( JMod.FINAL, object, "array" );

        m.javadoc().append( "Creates and returns a copy of a given array." );
        m.javadoc().addParam( arrayArg ).append( "The array to copy or {@code null}." );
        m.javadoc().addReturn().append( "A copy of {@code array} or {@code null} if {@code array} is {@code null}." );
        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional arrayNotNull = m.body()._if( arrayArg.ne( JExpr._null() ) );
        final JVar len = arrayNotNull._then().decl( JMod.FINAL, clazz.parent().getCodeModel().INT, "len",
                                                    array.staticInvoke( "getLength" ).arg( arrayArg ) );

        final JVar copy = arrayNotNull._then().decl( JMod.FINAL, object, "copy", array.staticInvoke( "newInstance" ).
            arg( arrayArg.invoke( "getClass" ).invoke( "getComponentType" ) ).arg( len ) );

        final JForLoop forEachRef = arrayNotNull._then()._for();
        final JVar i = forEachRef.init( clazz.parent().getCodeModel().INT, "i", len.minus( JExpr.lit( 1 ) ) );
        forEachRef.test( i.gte( JExpr.lit( 0 ) ) );
        forEachRef.update( i.decr() );
        forEachRef.body().add( array.staticInvoke( "set" ).arg( copy ).arg( i ).
            arg( array.staticInvoke( "get" ).arg( arrayArg ).arg( i ) ) );

        arrayNotNull._then()._return( copy );
        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfObjectInvocation( final ClassOutline clazz )
    {
        final JClass object = clazz.parent().getCodeModel().ref( Object.class );
        final JClass element = clazz.parent().getCodeModel().ref( Element.class );
        final JClass jaxbElement = clazz.parent().getCodeModel().ref( JAXBElement.class );
        final JClass noSuchMethod = clazz.parent().getCodeModel().ref( NoSuchMethodException.class );
        final JClass illegalAccess = clazz.parent().getCodeModel().ref( IllegalAccessException.class );
        final JClass invocationTarget = clazz.parent().getCodeModel().ref( InvocationTargetException.class );
        final JClass assertionError = clazz.parent().getCodeModel().ref( AssertionError.class );

        final String methodName = "copyOfObject";
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, object, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, object, methodName ) );

        final JVar o = m.param( JMod.FINAL, object, "o" );

        m.javadoc().append( "Creates and returns a copy of a given object." );
        m.javadoc().addParam( o ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append( "A copy of {@code o} or {@code null} if {@code o} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional objectNotNull = m.body()._if( o.ne( JExpr._null() ) );

        final JConditional isPrimitive =
            objectNotNull._then()._if( JExpr.invoke( JExpr.invoke( o, "getClass" ), "isPrimitive" ) );

        isPrimitive._then()._return( o );

        final JConditional isArray =
            objectNotNull._then()._if( JExpr.invoke( JExpr.invoke( o, "getClass" ), "isArray" ) );

        isArray._then()._return( this.getCopyOfArrayInvocation( clazz ).arg( o ) );

        final JConditional isImmutable = objectNotNull._then()._if(
            this.getIsImmutableObjectInvocation( clazz ).arg( o ) );

        isImmutable._then()._return( o );

        final JConditional instanceOfDOMElement = objectNotNull._then()._if( o._instanceof( element ) );
        instanceOfDOMElement._then()._return( this.getCopyOfDomElementInvocation( clazz ).
            arg( JExpr.cast( element, o ) ) );

        final JConditional instanceOfElement = objectNotNull._then()._if( o._instanceof( jaxbElement ) );
        instanceOfElement._then()._return( this.getCopyOfJaxbElementInvocation( clazz ).
            arg( JExpr.cast( jaxbElement, o ) ) );

        final JTryBlock tryCloneMethod = objectNotNull._then()._try();
        tryCloneMethod.body()._return( JExpr.invoke( JExpr.invoke( JExpr.invoke( o, "getClass" ), "getMethod" ).
            arg( "clone" ).arg( this.getNoClassesField( clazz ) ), "invoke" ).arg( this.getNoObjectsField( clazz ) ) );

        final JExpression assertionErrorMsg =
            JExpr.lit( "Unexpected instance during copying object '" ).plus( o ).plus( JExpr.lit( "'." ) );

        final JCatchBlock catchNoSuchMethod = tryCloneMethod._catch( noSuchMethod );
        catchNoSuchMethod.body().directStatement( "// Please report this at " +
                                                  this.getMessage( "bugtrackerUrl", null ) );

        catchNoSuchMethod.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchNoSuchMethod.param( "e" ) ) ) );

        final JCatchBlock catchIllegalAccess = tryCloneMethod._catch( illegalAccess );
        catchIllegalAccess.body().directStatement( "// Please report this at " +
                                                   this.getMessage( "bugtrackerUrl", null ) );

        catchIllegalAccess.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchIllegalAccess.param( "e" ) ) ) );

        final JCatchBlock catchInvocationTarget = tryCloneMethod._catch( invocationTarget );
        catchInvocationTarget.body().directStatement( "// Please report this at " +
                                                      this.getMessage( "bugtrackerUrl", null ) );

        catchInvocationTarget.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchInvocationTarget.param( "e" ) ) ) );

        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfClassInfoInvocation( final ClassOutline clazz, final CTypeInfo type )
    {
        final JType javaType = type.toType( clazz.parent(), Aspect.IMPLEMENTATION );
        final JType[] signature =
        {
            javaType
        };

        final String methodName = "copyOf" + this.getMethodNamePart( javaType );
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, javaType, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, javaType, methodName ) );

        final JVar param = m.param( JMod.FINAL, javaType, "clazz" );

        m.javadoc().append( "Creates and returns a copy of a given {@code " + javaType.binaryName() + "} instance." );
        m.javadoc().addParam( param ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append( "A copy of {@code clazz} or {@code null} if {@code clazz} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        m.body()._if( param.ne( JExpr._null() ) )._then()._return( JExpr.invoke( param, "clone" ) );
        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfClassInfoElementInvocation( final ClassOutline clazz, final CTypeInfo type )
    {
        final JType jaxbElement = clazz.parent().getCodeModel().ref( JAXBElement.class );
        final JType javaType = type.toType( clazz.parent(), Aspect.IMPLEMENTATION );
        final JType[] signature =
        {
            jaxbElement
        };

        final String methodName = "copyOf" + this.getMethodNamePart( javaType ) + "Element";
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, jaxbElement, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, jaxbElement, methodName ) );

        final JVar e = m.param( JMod.FINAL, jaxbElement, "e" );

        m.javadoc().append( "Creates and returns a copy of a given {@code JAXBElement<" + javaType.binaryName() +
                            ">} instance." );

        m.javadoc().addParam( e ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append( "A copy of {@code e} or {@code null} if {@code e} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional elementNotNull = m.body()._if( e.ne( JExpr._null() ) );

        final JExpression newElement = JExpr._new( jaxbElement ).
            arg( JExpr.invoke( e, "getName" ) ).
            arg( JExpr.invoke( e, "getDeclaredType" ) ).
            arg( JExpr.invoke( e, "getScope" ) ).
            arg( JExpr.invoke( e, "getValue" ) );

        final JVar copy = elementNotNull._then().decl( jaxbElement, "copy", newElement );
        elementNotNull._then().add( copy.invoke( "setNil" ).arg( e.invoke( "isNil" ) ) );
        elementNotNull._then().add( copy.invoke( "setValue" ).arg( this.getCopyExpression(
            clazz, type, elementNotNull._then(), JExpr.cast( javaType, copy.invoke( "getValue" ) ) ) ) );

        elementNotNull._then()._return( copy );
        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfArrayInfoInvocation( final ClassOutline clazz, final CArrayInfo array )
    {
        final JType arrayType =
            ( array.getAdapterUse() != null ? clazz.parent().getModel().getTypeInfo( array.getAdapterUse().customType ).
            toType( clazz.parent(), Aspect.IMPLEMENTATION ) : array.toType( clazz.parent(), Aspect.IMPLEMENTATION ) );

        final JType itemType = array.getItemType().toType( clazz.parent(), Aspect.IMPLEMENTATION );
        final JType[] signature =
        {
            arrayType
        };

        final String methodName = "copyOf" + this.getMethodNamePart( arrayType );
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : clazz._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return clazz._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : clazz.implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, arrayType, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, arrayType, methodName ) );

        final JVar a = m.param( JMod.FINAL, arrayType, "array" );

        m.javadoc().append( "Creates and returns a copy of a given {@code " + arrayType.binaryName() + "} instance." );
        m.javadoc().addParam( a ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append( "A copy of {@code array} or {@code null} if {@code array} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional arrayNotNull = m.body()._if( a.ne( JExpr._null() ) );
        final JVar copy = arrayNotNull._then().decl( arrayType, "copy", JExpr.newArray( itemType, a.ref( "length" ) ) );
        final JForLoop forEachItem = arrayNotNull._then()._for();
        final JVar i = forEachItem.init(
            clazz.parent().getCodeModel().INT, "i", a.ref( "length" ).minus( JExpr.lit( 1 ) ) );

        forEachItem.test( i.gte( JExpr.lit( 0 ) ) );
        forEachItem.update( i.decr() );

        final JExpression copyExpr =
            this.getCopyExpression( clazz, array.getItemType(), forEachItem.body(), a.component( i ) );

        forEachItem.body().assign( copy.component( i ), copyExpr );
        arrayNotNull._then()._return( copy );
        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JMethod getCopyOfCollectionMethod( final FieldOutline field )
    {
        final String methodName = "copy" + field.getPropertyInfo().getName( true );
        for ( JMethod m : field.parent().implClass.methods() )
        {
            if ( m.name().equals( methodName ) )
            {
                return m;
            }
        }

        final JClass object = field.parent().parent().getCodeModel().ref( Object.class );
        final JClass array = field.parent().parent().getCodeModel().ref( Array.class );
        final JClass jaxbElement = field.parent().parent().getCodeModel().ref( JAXBElement.class );
        final JClass nullPointerException = field.parent().parent().getCodeModel().ref( NullPointerException.class );
        final JClass assertionError = field.parent().parent().getCodeModel().ref( AssertionError.class );
        final JMethod m = field.parent().implClass.method(
            field.getRawType().isArray() ? this.getVisibilityModifier() : this.getVisibilityModifier() | JMod.STATIC,
            Void.TYPE, methodName );

        final JVar source = m.param( JMod.FINAL, field.getRawType(), "source" );
        final JVar target = field.getRawType().isArray() ? null : m.param( JMod.FINAL, field.getRawType(), "target" );

        m.javadoc().append( "Copies all values of property {@code " + field.getPropertyInfo().getName( true ) + "}." );
        m.javadoc().addParam( source ).append( "The source to copy from." );

        if ( !field.getRawType().isArray() )
        {
            m.javadoc().addParam( target ).append( "The target to copy {@code source} to." );
            m.javadoc().addThrows( nullPointerException ).
                append( "if {@code source} or {@code target} is {@code null}." );

        }
        else
        {
            m.javadoc().addThrows( nullPointerException ).append( "if {@code source} is {@code null}." );
        }

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

//        m.body()._if( source.eq( JExpr._null() ) )._then()._throw( JExpr._new( nullPointerException ).arg( "source" ) );
//        m.body()._if( target.eq( JExpr._null() ) )._then()._throw( JExpr._new( nullPointerException ).arg( "target" ) );

        final List<CClassInfo> referencedClassInfos =
            new ArrayList<CClassInfo>( field.getPropertyInfo().ref().size() );

        final List<CElementInfo> referencedElementInfos =
            new ArrayList<CElementInfo>( field.getPropertyInfo().ref().size() );

        final List<CTypeInfo> referencedTypeInfos =
            new ArrayList<CTypeInfo>( field.getPropertyInfo().ref().size() );

        final List<JType> referencedClassTypes =
            new ArrayList<JType>( field.getPropertyInfo().ref().size() );

        final List<JType> referencedContentTypes =
            new ArrayList<JType>( field.getPropertyInfo().ref().size() );

        final List<JType> referencedTypes =
            new ArrayList<JType>( field.getPropertyInfo().ref().size() );

        for ( CTypeInfo type : field.getPropertyInfo().ref() )
        {
            if ( type instanceof CElementInfo )
            {
                final CElementInfo e = (CElementInfo) type;
                final JType contentType = e.getContentType().toType( field.parent().parent(), Aspect.IMPLEMENTATION );

                if ( !referencedContentTypes.contains( contentType ) )
                {
                    referencedContentTypes.add( contentType );
                    referencedElementInfos.add( e );
                }
            }
            else if ( type instanceof CClassInfo )
            {
                final CClassInfo c = (CClassInfo) type;
                final JClass classType = c.toType( field.parent().parent(), Aspect.IMPLEMENTATION );

                if ( !referencedClassTypes.contains( classType ) )
                {
                    referencedClassTypes.add( classType );
                    referencedClassInfos.add( c );
                }
            }
            else
            {
                final JType javaType = type.toType( field.parent().parent(), Aspect.IMPLEMENTATION );
                if ( !referencedTypes.contains( javaType ) )
                {
                    referencedTypes.add( javaType );
                    referencedTypeInfos.add( type );
                }
            }
        }

        Collections.sort( referencedClassInfos, new CClassInfoComparator( field.parent().parent() ) );
        Collections.sort( referencedElementInfos, new CElementInfoComparator( field.parent().parent() ) );
        Collections.sort( referencedTypeInfos, new CTypeInfoComparator( field.parent().parent() ) );

        final JForLoop copyLoop;
        final JVar it;
        final JVar next;
        final JVar copy;
        final JConditional sourceNotEmpty;

        if ( field.getRawType().isArray() )
        {
            sourceNotEmpty =
                m.body()._if( source.ne( JExpr._null() ).cand( source.ref( "length" ).gt( JExpr.lit( 0 ) ) ) );

            copy = sourceNotEmpty._then().decl( JMod.FINAL, source.type(), "copy", JExpr.cast(
                source.type(), array.staticInvoke( "newInstance" ).
                arg( source.invoke( "getClass" ).invoke( "getComponentType" ) ).arg( source.ref( "length" ) ) ) );

            copyLoop = sourceNotEmpty._then()._for();
            it = copyLoop.init( field.parent().parent().getCodeModel().INT, "i",
                                source.ref( "length" ).minus( JExpr.lit( 1 ) ) );

            copyLoop.test( it.gte( JExpr.lit( 0 ) ) );
            copyLoop.update( it.decr() );
            next = copyLoop.body().decl( JMod.FINAL, object, "next", source.component( it ) );
        }
        else
        {
            sourceNotEmpty = m.body()._if( JExpr.invoke( source, "isEmpty" ).ne( JExpr.TRUE ) );
            copyLoop = sourceNotEmpty._then()._for();
            it = copyLoop.init( field.parent().parent().getCodeModel().ref( Iterator.class ),
                                "it", source.invoke( "iterator" ) );

            copyLoop.test( JExpr.invoke( it, "hasNext" ) );
            next = copyLoop.body().decl( JMod.FINAL, object, "next", JExpr.invoke( it, "next" ) );
            copy = null;
        }

        for ( CClassInfo classInfo : referencedClassInfos )
        {
            final JType javaType =
                ( classInfo.getAdapterUse() != null ? field.parent().parent().getModel().getTypeInfo( classInfo.
                getAdapterUse().customType ).toType( field.parent().parent(), Aspect.IMPLEMENTATION )
                  : classInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION ) );

            final JConditional ifInstanceOf = copyLoop.body()._if( next._instanceof( javaType ) );

            final JExpression copyExpr = this.getCopyExpression(
                field.parent(), classInfo, ifInstanceOf._then(), JExpr.cast( javaType, next ) );

            if ( copyExpr == null )
            {
                this.log( Level.SEVERE, this.getMessage( "cannotCopyProperty", new Object[]
                    {
                        field.getPropertyInfo().getName( true ),
                        field.parent().implClass.binaryName()
                    } ), null );

            }
            else
            {
                if ( field.getRawType().isArray() )
                {
                    ifInstanceOf._then().assign( copy.component( it ), copyExpr );
                }
                else
                {
                    ifInstanceOf._then().invoke( target, "add" ).arg( copyExpr );
                }
            }

            ifInstanceOf._then()._continue();
        }

        if ( !referencedElementInfos.isEmpty() )
        {
            final JBlock copyBlock = copyLoop.body()._if( next._instanceof( jaxbElement ) )._then();

            for ( CElementInfo elementInfo : referencedElementInfos )
            {
                final JType contentType =
                    ( elementInfo.getAdapterUse() != null ? field.parent().parent().getModel().getTypeInfo(
                    elementInfo.getAdapterUse().customType ).toType( field.parent().parent(), Aspect.IMPLEMENTATION )
                      : elementInfo.getContentType().toType( field.parent().parent(), Aspect.IMPLEMENTATION ) );

                final JConditional ifInstanceOf = copyBlock._if( JExpr.invoke( JExpr.cast(
                    jaxbElement, next ), "getValue" )._instanceof( contentType ) );

                final JExpression copyExpr = this.getCopyExpression(
                    field.parent(), elementInfo, ifInstanceOf._then(), JExpr.cast( jaxbElement, next ) );

                if ( copyExpr == null )
                {
                    this.log( Level.SEVERE, this.getMessage( "cannotCopyProperty", new Object[]
                        {
                            field.getPropertyInfo().getName( true ),
                            field.parent().implClass.binaryName()
                        } ), null );

                }
                else
                {
                    if ( field.getRawType().isArray() )
                    {
                        ifInstanceOf._then().assign( copy.component( it ), copyExpr );
                    }
                    else
                    {
                        ifInstanceOf._then().invoke( target, "add" ).arg( copyExpr );
                    }
                }

                ifInstanceOf._then()._continue();
            }
        }

        for ( CTypeInfo typeInfo : referencedTypeInfos )
        {
            final JType javaType = typeInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION );
            final JConditional ifInstanceOf = copyLoop.body()._if( next._instanceof( javaType ) );
            final JExpression copyExpr = this.getCopyExpression(
                field.parent(), typeInfo, ifInstanceOf._then(), JExpr.cast( javaType, next ) );

            if ( copyExpr == null )
            {
                this.log( Level.SEVERE, this.getMessage( "cannotCopyProperty", new Object[]
                    {
                        field.getPropertyInfo().getName( true ),
                        field.parent().implClass.binaryName()
                    } ), null );

            }
            else
            {
                if ( field.getRawType().isArray() )
                {
                    ifInstanceOf._then().assign( copy.component( it ), copyExpr );
                }
                else
                {
                    ifInstanceOf._then().invoke( target, "add" ).arg( copyExpr );
                }
            }

            ifInstanceOf._then()._continue();
        }

        copyLoop.body().directStatement( "// Please report this at " + this.getMessage( "bugtrackerUrl", null ) );
        copyLoop.body()._throw( JExpr._new( assertionError ).arg( JExpr.lit( "Unexpected instance '" ).plus(
            next ).plus( JExpr.lit( "' for property '" + field.getPropertyInfo().getName( true ) + "' of class '" +
                                    field.parent().implClass.binaryName() + "'." ) ) ) );

        if ( field.getRawType().isArray() )
        {
            sourceNotEmpty._then().add( JExpr.invoke( "set" + field.getPropertyInfo().getName( true ) ).arg( copy ) );
        }

        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return m;
    }

    private JExpression getCopyExpression( final ClassOutline classOutline, final CTypeInfo type,
                                           final JBlock block, final JExpression source )
    {
        JExpression expr = null;

        if ( type instanceof CBuiltinLeafInfo )
        {
            expr = this.getBuiltinCopyExpression( classOutline, (CBuiltinLeafInfo) type, block, source );
        }
        else if ( type instanceof CWildcardTypeInfo )
        {
            expr = this.getWildcardCopyExpression( classOutline, (CWildcardTypeInfo) type, block, source );
        }
        else if ( type instanceof CClassInfo )
        {
            expr = this.getClassInfoCopyExpression( classOutline, (CClassInfo) type, block, source );
        }
        else if ( type instanceof CEnumLeafInfo )
        {
            expr = this.getEnumLeafInfoCopyExpression( classOutline, (CEnumLeafInfo) type, block, source );
        }
        else if ( type instanceof CArrayInfo )
        {
            expr = this.getArrayCopyExpression( classOutline, (CArrayInfo) type, block, source );
        }
        else if ( type instanceof CElementInfo )
        {
            expr = this.getElementCopyExpression( classOutline, (CElementInfo) type, block, source );
        }
        else if ( type instanceof CNonElement )
        {
            expr = this.getNonElementCopyExpression( classOutline, (CNonElement) type, block, source );
        }

        if ( expr != null )
        {
            this.expressionCount = this.expressionCount.add( BigInteger.ONE );
        }

        return expr;
    }

    private JExpression getBuiltinCopyExpression( final ClassOutline classOutline, final CBuiltinLeafInfo type,
                                                  final JBlock block, final JExpression source )
    {
        JExpression expr = null;

        block.directStatement(
            "// CBuiltinLeafInfo: " + type.toType( classOutline.parent(), Aspect.IMPLEMENTATION ).binaryName() );

        if ( type == CBuiltinLeafInfo.ANYTYPE )
        {
            expr = this.getCopyOfObjectInvocation( classOutline ).arg( source );
        }
        else if ( type == CBuiltinLeafInfo.BASE64_BYTE_ARRAY )
        {
            final JType byteArray = classOutline.parent().getCodeModel().ref( byte[].class );
            expr = JExpr.cast( byteArray, this.getCopyOfArrayInvocation( classOutline ).arg( source ) );
        }
        else if ( type == CBuiltinLeafInfo.BIG_DECIMAL || type == CBuiltinLeafInfo.BIG_INTEGER ||
                  type == CBuiltinLeafInfo.STRING || type == CBuiltinLeafInfo.BOOLEAN || type == CBuiltinLeafInfo.INT ||
                  type == CBuiltinLeafInfo.LONG || type == CBuiltinLeafInfo.BYTE || type == CBuiltinLeafInfo.SHORT ||
                  type == CBuiltinLeafInfo.FLOAT || type == CBuiltinLeafInfo.DOUBLE )
        {
            expr = source;
        }
        else if ( type == CBuiltinLeafInfo.QNAME )
        {
            expr = this.getCopyOfQNameInvocation( classOutline ).arg( source );
        }
        else if ( type == CBuiltinLeafInfo.CALENDAR )
        {
            expr = this.getCopyOfXMLGregorianCalendarInvocation( classOutline ).arg( source );
        }
        else if ( type == CBuiltinLeafInfo.DURATION )
        {
            expr = this.getCopyOfDurationInvocation( classOutline ).arg( source );
        }
        else if ( type == CBuiltinLeafInfo.DATA_HANDLER || type == CBuiltinLeafInfo.IMAGE ||
                  type == CBuiltinLeafInfo.XML_SOURCE )
        {
            expr = source;
        }

        return expr;
    }

    private JExpression getWildcardCopyExpression( final ClassOutline classOutline, final CWildcardTypeInfo type,
                                                   final JBlock block, final JExpression source )
    {
        block.directStatement( "// CWildcardTypeInfo: " +
                               type.toType( classOutline.parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return this.getCopyOfDomElementInvocation( classOutline ).arg( source );
    }

    private JExpression getClassInfoCopyExpression( final ClassOutline classOutline, final CClassInfo type,
                                                    final JBlock block, final JExpression source )
    {
        block.directStatement(
            "// CClassInfo: " + type.toType( classOutline.parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return this.getCopyOfClassInfoInvocation( classOutline, type ).arg( source );
    }

    private JExpression getNonElementCopyExpression( final ClassOutline classOutline, final CNonElement type,
                                                     final JBlock block, final JExpression source )
    {
        block.directStatement(
            "// CNonElement: " + type.toType( classOutline.parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return null;
    }

    private JExpression getArrayCopyExpression( final ClassOutline classOutline, final CArrayInfo type,
                                                final JBlock block, final JExpression source )
    {
        block.directStatement( "// CArrayInfo: " + type.fullName() );
        return this.getCopyOfArrayInfoInvocation( classOutline, type ).arg( source );
    }

    private JExpression getElementCopyExpression( final ClassOutline classOutline, final CElementInfo type,
                                                  final JBlock block, final JExpression source )
    {
        block.directStatement(
            "// CElementInfo: " + type.toType( classOutline.parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return this.getCopyOfClassInfoElementInvocation( classOutline, type.getContentType() ).arg( source );
    }

    private JExpression getEnumLeafInfoCopyExpression( final ClassOutline classOutline, final CEnumLeafInfo type,
                                                       final JBlock block, final JExpression source )
    {
        block.directStatement(
            "// CEnumLeafInfo: " + type.toType( classOutline.parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return source;
    }

    private JMethod generateStandardConstructor( final ClassOutline clazz )
    {
        final JMethod ctor = clazz.implClass.constructor( JMod.PUBLIC );
        ctor.body().directStatement( " // " + this.getMessage( "title", null ) );
        ctor.body().invoke( "super" );
        ctor.javadoc().add( "Creates a new {@code " + clazz.implClass.fullName() + "} instance." );
        this.constructorCount = this.constructorCount.add( BigInteger.ONE );
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

        boolean hasFields = false;
        if ( !clazz.implClass.fields().isEmpty() )
        {
            final JBlock paramNotNullBlock = new JBlock( true, true );

            for ( FieldOutline field : clazz.getDeclaredFields() )
            {
                hasFields = true;
                this.generateCopyOfProperty( field, o, paramNotNullBlock );
            }

            for ( JFieldVar field : clazz.implClass.fields().values() )
            {
                if ( ( field.mods().getValue() & JMod.STATIC ) == JMod.STATIC )
                {
                    continue;
                }

                hasFields = true;
                final FieldOutline fieldOutline = this.getFieldOutline( clazz, field.name() );
                if ( fieldOutline == null )
                {
                    if ( field.type().isPrimitive() )
                    {
                        paramNotNullBlock.assign( JExpr.refthis( field.name() ), o.ref( field ) );
                    }
                    else
                    {
                        paramNotNullBlock.assign( JExpr.refthis( field.name() ), JExpr.cast(
                            field.type(), this.getCopyOfObjectInvocation( clazz ).arg( o.ref( field ) ) ) );

                    }
                }
            }

            if ( hasFields )
            {
                ctor.body()._if( o.ne( JExpr._null() ) )._then().add( paramNotNullBlock );
            }
        }

        this.constructorCount = this.constructorCount.add( BigInteger.ONE );
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
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return cloneMethod;
    }

    private void generateCopyOfProperty( final FieldOutline field, final JVar o, final JBlock block )
    {
        final JMethod getter = this.getPropertyGetter( field );

        if ( getter != null )
        {
            if ( field.getPropertyInfo().isCollection() )
            {
                if ( field.getRawType().isArray() )
                {
                    block.directStatement( "// '" + field.getPropertyInfo().getName( true ) + "' array." );
                    block.add( JExpr.invoke( this.getCopyOfCollectionMethod( field ) ).
                        arg( JExpr.invoke( o, getter ) ) );

                }
                else
                {
                    block.directStatement( "// '" + field.getPropertyInfo().getName( true ) + "' collection." );
                    block.add( JExpr.invoke( this.getCopyOfCollectionMethod( field ) ).
                        arg( JExpr.invoke( o, getter ) ).arg( JExpr.invoke( getter ) ) );

                }
            }
            else
            {
                final CTypeInfo typeInfo =
                    ( field.getPropertyInfo().getAdapter() != null ? field.parent().parent().getModel().getTypeInfo(
                    field.getPropertyInfo().getAdapter().customType ) : field.getPropertyInfo().ref().iterator().next() );

                final JType javaType = typeInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION );
                final JExpression copyExpr = this.getCopyExpression( field.parent(), typeInfo, block, JExpr.cast(
                    javaType, JExpr.invoke( o, getter ) ) );

                if ( copyExpr == null )
                {
                    this.log( Level.SEVERE, this.getMessage( "cannotCopyProperty", new Object[]
                        {
                            field.getPropertyInfo().getName( true ),
                            field.parent().implClass.binaryName()
                        } ), null );

                }
                else
                {
                    block.assign( JExpr.refthis( field.getPropertyInfo().getName( false ) ), copyExpr );
                }
            }
        }
        else
        {
            throw new AssertionError( this.getMessage( "getterNotFound", new Object[]
                {
                    field.getPropertyInfo().getName( true ),
                    field.parent().implClass.binaryName(),
                } ) );

        }
    }

    private String getMethodNamePart( final JType type )
    {
        String methodName = type.name();
        if ( type.isArray() )
        {
            methodName = methodName.replace( "[]", "s" );
        }
        final char[] c = methodName.toCharArray();
        c[0] = Character.toUpperCase( c[0] );
        methodName = String.valueOf( c );
        return methodName;
    }

    private String getMessage( final String key, final Object args )
    {
        final ResourceBundle bundle = ResourceBundle.getBundle( "net/sourceforge/ccxjc/PluginImpl" );
        return new MessageFormat( bundle.getString( key ) ).format( args );
    }

    private void log( final Level level, final String key, final Object args )
    {
        final StringBuffer b = new StringBuffer().append( "[" ).append( MESSAGE_PREFIX ).append( "] [" ).
            append( level.getLocalizedName() ).append( "] " ).append( this.getMessage( key, args ) );

        if ( this.options != null && !this.options.quiet )
        {
            final int l = this.options != null && this.options.debugMode ? Level.ALL.intValue() : Level.INFO.intValue();
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

class CClassInfoComparator implements Comparator<CClassInfo>
{

    private final Outline outline;

    CClassInfoComparator( final Outline outline )
    {
        this.outline = outline;
    }

    private int recurse( final JClass o1, final JClass o2, final int hierarchy )
    {
        if ( o1.binaryName().equals( o2.binaryName() ) )
        {
            return hierarchy;
        }
        if ( o2._extends() != null && !o2._extends().binaryName().equals( "java.lang.Object" ) )
        {
            return this.recurse( o1, o2._extends(), hierarchy + 1 );
        }

        return 0;
    }

    public int compare( final CClassInfo o1, final CClassInfo o2 )
    {
        final JClass javaClass1 = o1.toType( this.outline, Aspect.IMPLEMENTATION );
        final JClass javaClass2 = o2.toType( this.outline, Aspect.IMPLEMENTATION );

        final int ret = this.recurse( javaClass1, javaClass2, 0 );
        return ret;
    }

}

class CElementInfoComparator implements Comparator<CElementInfo>
{

    private final Outline outline;

    CElementInfoComparator( final Outline outline )
    {
        this.outline = outline;
    }

    private int recurse( final JClass o1, final JClass o2, final int hierarchy )
    {
        if ( o1.binaryName().equals( o2.binaryName() ) )
        {
            return hierarchy;
        }
        if ( o2._extends() != null && !o2._extends().binaryName().equals( "java.lang.Object" ) )
        {
            return this.recurse( o1, o2._extends(), hierarchy + 1 );
        }

        return 0;
    }

    public int compare( final CElementInfo o1, final CElementInfo o2 )
    {
        final JClass javaClass1 = (JClass) o1.getContentType().toType( this.outline, Aspect.IMPLEMENTATION );
        final JClass javaClass2 = (JClass) o2.getContentType().toType( this.outline, Aspect.IMPLEMENTATION );

        final int ret = this.recurse( javaClass1, javaClass2, 0 );
        return ret;
    }

}

class CTypeInfoComparator implements Comparator<CTypeInfo>
{

    private final Outline outline;

    CTypeInfoComparator( final Outline outline )
    {
        this.outline = outline;
    }

    private int recurse( final JClass o1, final JClass o2, final int hierarchy )
    {
        if ( o1.binaryName().equals( o2.binaryName() ) )
        {
            return hierarchy;
        }
        if ( o2._extends() != null && !o2._extends().binaryName().equals( "java.lang.Object" ) )
        {
            return this.recurse( o1, o2._extends(), hierarchy + 1 );
        }

        return 0;
    }

    public int compare( final CTypeInfo o1, final CTypeInfo o2 )
    {
        final JType javaType1 = o1.toType( this.outline, Aspect.IMPLEMENTATION );
        final JType javaType2 = o2.toType( this.outline, Aspect.IMPLEMENTATION );

        int ret = 0;
        if ( javaType1 instanceof JClass && javaType2 instanceof JClass )
        {
            ret = this.recurse( (JClass) javaType1, (JClass) javaType2, 0 );
        }

        return ret;
    }

}
