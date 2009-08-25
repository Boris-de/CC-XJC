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
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JForLoop;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
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

    private static final String OPTION_NAME = "copy-constructor";

    private static final String VISIBILITY_OPTION_NAME = "-cc-visibility";

    private static final String TARGET_OPTION_NAME = "-cc-target";

    private static final Class[] IMMUTABLE_TYPES =
    {
        Boolean.class,
        Byte.class,
        Character.class,
        Double.class,
        Enum.class,
        Float.class,
        Integer.class,
        Long.class,
        Short.class,
        String.class,
        BigDecimal.class,
        BigInteger.class,
        UUID.class,
        QName.class,
        Duration.class,
        Currency.class
    };

    private static final Class[] CLONEABLE_TYPES =
    {
        XMLGregorianCalendar.class,
        Date.class,
        Calendar.class,
        TimeZone.class,
        Locale.class
    };

    private static final Class[] PRIMITIVE_ARRAY_TYPES =
    {
        boolean[].class,
        byte[].class,
        char[].class,
        double[].class,
        float[].class,
        int[].class,
        long[].class,
        short[].class
    };

    private static final String[] VISIBILITY_ARGUMENTS =
    {
        "private", "package", "protected", "public"
    };

    private static final String[] TARGET_ARGUMENTS =
    {
        "1.5", "1.6", "1.7"
    };

    private static final int TARGET_1_5 = 5;

    private static final int TARGET_1_6 = 6;

    private static final int TARGET_1_7 = 7;

    private boolean success;

    private Options options;

    private String visibility = "private";

    private int targetJdk = TARGET_1_5;

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
        return new StringBuffer().append( "  -" ).append( OPTION_NAME ).append( "  :  " ).
            append( this.getMessage( "usage", null ) ).append( System.getProperty( "line.separator" ) ).
            append( "  " ).append( VISIBILITY_OPTION_NAME ).append( "     :  " ).
            append( this.getMessage( "visibilityUsage", null ) ).append( System.getProperty( "line.separator" ) ).
            append( "  " ).append( TARGET_OPTION_NAME ).append( "         :  " ).
            append( this.getMessage( "targetUsage", null ) ).toString();

    }

    @Override
    public int parseArgument( final Options opt, final String[] args, final int i )
        throws BadCommandLineException, IOException
    {
        final StringBuffer supportedVisibilities = new StringBuffer().append( '[' );
        for ( Iterator<String> it = Arrays.asList( VISIBILITY_ARGUMENTS ).iterator(); it.hasNext(); )
        {
            supportedVisibilities.append( it.next() );
            if ( it.hasNext() )
            {
                supportedVisibilities.append( ", " );
            }
        }

        final StringBuffer supportedTargets = new StringBuffer().append( '[' );
        for ( Iterator<String> it = Arrays.asList( TARGET_ARGUMENTS ).iterator(); it.hasNext(); )
        {
            supportedTargets.append( it.next() );
            if ( it.hasNext() )
            {
                supportedTargets.append( ", " );
            }
        }

        if ( args[i].startsWith( VISIBILITY_OPTION_NAME ) )
        {
            if ( i + 1 >= args.length )
            {
                final String missingOptionArgument = this.getMessage( "missingOptionArgument", new Object[]
                    {
                        VISIBILITY_OPTION_NAME
                    } );

                final String expectedOptionArgument = this.getMessage( "expectedOptionArgument", new Object[]
                    {
                        supportedVisibilities.append( ']' ).toString()
                    } );

                throw new BadCommandLineException( missingOptionArgument + " " + expectedOptionArgument );
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
                final String expectedOptionArgument = this.getMessage( "expectedOptionArgument", new Object[]
                    {
                        supportedVisibilities.append( ']' ).toString()
                    } );

                throw new BadCommandLineException( expectedOptionArgument );
            }

            return 2;
        }

        if ( args[i].startsWith( TARGET_OPTION_NAME ) )
        {
            if ( i + 1 >= args.length )
            {
                final String missingOptionArgument = this.getMessage( "missingOptionArgument", new Object[]
                    {
                        TARGET_OPTION_NAME
                    } );

                final String expectedOptionArgument = this.getMessage( "expectedOptionArgument", new Object[]
                    {
                        supportedTargets.append( ']' ).toString()
                    } );

                throw new BadCommandLineException( missingOptionArgument + " " + expectedOptionArgument );
            }

            final String targetArg = args[i + 1].trim();

            boolean supported = false;
            for ( String argument : TARGET_ARGUMENTS )
            {
                if ( argument.equals( targetArg ) )
                {
                    supported = true;
                    break;
                }
            }

            if ( !supported )
            {
                final String expectedOptionArgument = this.getMessage( "expectedOptionArgument", new Object[]
                    {
                        supportedTargets.append( ']' ).toString()
                    } );

                throw new BadCommandLineException( expectedOptionArgument );
            }

            if ( targetArg.equals( "1.5" ) )
            {
                this.targetJdk = TARGET_1_5;
            }
            else if ( targetArg.equals( "1.6" ) )
            {
                this.targetJdk = TARGET_1_6;
            }
            else if ( targetArg.equals( "1.7" ) )
            {
                this.targetJdk = TARGET_1_7;
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

    private boolean isTargetSupported( final int target )
    {
        return target <= this.targetJdk;
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

        m.javadoc().append( "Creates and returns a deep copy of a given {@code JAXBElement} instance." );
        m.javadoc().addParam( element ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append(
            "A deep copy of {@code element} or {@code null} if {@code element} is {@code null}." );

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

    private JExpression getCopyOfPrimitiveArrayExpression( final ClassOutline classOutline, final JClass arrayType,
                                                           final JExpression source )
    {
        if ( this.isTargetSupported( TARGET_1_6 ) )
        {
            final JClass arrays = classOutline.parent().getCodeModel().ref( Arrays.class );
            return JOp.cond( source.eq( JExpr._null() ), JExpr._null(), arrays.staticInvoke( "copyOf" ).
                arg( source ).arg( source.ref( "length" ) ) );

        }

        final JClass array = classOutline.parent().getCodeModel().ref( Array.class );
        final JClass system = classOutline.parent().getCodeModel().ref( System.class );

        final int mod = this.getVisibilityModifier();
        final String methodName = "copyOf";

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : classOutline._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) )
                {
                    final JType[] signature = m.listParamTypes();
                    if ( signature.length == 1 && signature[0].binaryName().equals( arrayType.binaryName() ) )
                    {
                        return classOutline._package().objectFactory().staticInvoke( m ).arg( source );
                    }
                }
            }
        }
        else
        {
            for ( JMethod m : classOutline.implClass.methods() )
            {
                if ( m.name().equals( methodName ) )
                {
                    final JType[] signature = m.listParamTypes();
                    if ( signature.length == 1 && signature[0].binaryName().equals( arrayType.binaryName() ) )
                    {
                        return JExpr.invoke( m ).arg( source );
                    }
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? classOutline._package().objectFactory().method( JMod.STATIC | mod, arrayType, methodName )
              : classOutline.implClass.method( JMod.STATIC | mod, arrayType, methodName ) );

        final JVar arrayParam = m.param( JMod.FINAL, arrayType, "array" );

        m.javadoc().append( "Creates and returns a deep copy of a given array." );
        m.javadoc().addParam( arrayParam ).append( "The array to copy or {@code null}." );
        m.javadoc().addReturn().append(
            "A deep copy of {@code array} or {@code null} if {@code array} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional arrayNotNull = m.body()._if( arrayParam.ne( JExpr._null() ) );
        final JVar copy = arrayNotNull._then().decl(
            JMod.FINAL, arrayType, "copy", JExpr.cast( arrayType, array.staticInvoke( "newInstance" ).arg(
            arrayParam.invoke( "getClass" ).invoke( "getComponentType" ) ).arg( arrayParam.ref( "length" ) ) ) );

        arrayNotNull._then().add( system.staticInvoke( "arraycopy" ).arg( arrayParam ).arg( JExpr.lit( 0 ) ).
            arg( copy ).arg( JExpr.lit( 0 ) ).arg( arrayParam.ref( "length" ) ) );

        arrayNotNull._then()._return( copy );

        m.body()._return( JExpr._null() );

        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? classOutline._package().objectFactory().staticInvoke( m ).arg( source )
                 : JExpr.invoke( m ).arg( source ) );

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

        m.javadoc().append( "Creates and returns a deep copy of a given array." );
        m.javadoc().addParam( arrayArg ).append( "The array to copy or {@code null}." );
        m.javadoc().addReturn().append(
            "A deep copy of {@code array} or {@code null} if {@code array} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional arrayNotNull = m.body()._if( arrayArg.ne( JExpr._null() ) );

        for ( Class a : PRIMITIVE_ARRAY_TYPES )
        {
            final JClass primitiveArray = clazz.parent().getCodeModel().ref( a );
            final JConditional isArrayOfPrimitive =
                arrayNotNull._then()._if( arrayArg.invoke( "getClass" ).eq( primitiveArray.dotclass() ) );

            isArrayOfPrimitive._then()._return( this.getCopyOfPrimitiveArrayExpression(
                clazz, primitiveArray, JExpr.cast( primitiveArray, arrayArg ) ) );

        }

        final JVar len = arrayNotNull._then().decl(
            JMod.FINAL, clazz.parent().getCodeModel().INT, "len", array.staticInvoke( "getLength" ).
            arg( arrayArg ) );

        final JVar copy = arrayNotNull._then().decl( JMod.FINAL, object, "copy", array.staticInvoke( "newInstance" ).
            arg( arrayArg.invoke( "getClass" ).invoke( "getComponentType" ) ).arg( len ) );

        final JForLoop forEachRef = arrayNotNull._then()._for();
        final JVar i = forEachRef.init( clazz.parent().getCodeModel().INT, "i", len.minus( JExpr.lit( 1 ) ) );
        forEachRef.test( i.gte( JExpr.lit( 0 ) ) );
        forEachRef.update( i.decr() );
        forEachRef.body().add( array.staticInvoke( "set" ).arg( copy ).arg( i ).
            arg( this.getCopyOfObjectInvocation( clazz ).arg( array.staticInvoke( "get" ).
            arg( arrayArg ).arg( i ) ) ) );

        arrayNotNull._then()._return( copy );
        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfSerializableInvocation( final ClassOutline clazz )
    {
        final JClass serializable = clazz.parent().getCodeModel().ref( Serializable.class );
        final JClass byteArrayOutputStream = clazz.parent().getCodeModel().ref( ByteArrayOutputStream.class );
        final JClass byteArrayInputStream = clazz.parent().getCodeModel().ref( ByteArrayInputStream.class );
        final JClass objectOutputStream = clazz.parent().getCodeModel().ref( ObjectOutputStream.class );
        final JClass objectInputStream = clazz.parent().getCodeModel().ref( ObjectInputStream.class );
        final JClass ioException = clazz.parent().getCodeModel().ref( IOException.class );
        final JClass invalidClass = clazz.parent().getCodeModel().ref( InvalidClassException.class );
        final JClass notSerializable = clazz.parent().getCodeModel().ref( NotSerializableException.class );
        final JClass streamCorrupted = clazz.parent().getCodeModel().ref( StreamCorruptedException.class );
        final JClass securityException = clazz.parent().getCodeModel().ref( SecurityException.class );
        final JClass optionalData = clazz.parent().getCodeModel().ref( OptionalDataException.class );
        final JClass classNotFound = clazz.parent().getCodeModel().ref( ClassNotFoundException.class );
        final JClass assertionError = clazz.parent().getCodeModel().ref( AssertionError.class );
        final JType[] signature =
        {
            serializable
        };

        final String methodName = "copyOfSerializable";
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
              ? clazz._package().objectFactory().method( JMod.STATIC | mod, serializable, methodName )
              : clazz.implClass.method( JMod.STATIC | mod, serializable, methodName ) );

        final JVar s = m.param( JMod.FINAL, serializable, "serializable" );

        m.javadoc().append( "Creates and returns a deep copy of a given {@code Serializable}." );
        m.javadoc().addParam( s ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append(
            "A deep copy of {@code serializable} or {@code null} if {@code serializable} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional sNotNull = m.body()._if( s.ne( JExpr._null() ) );
        final JTryBlock tryClone = sNotNull._then()._try();

        final JVar byteArrayOutput = tryClone.body().decl(
            JMod.FINAL, byteArrayOutputStream, "byteArrayOutput", JExpr._new( byteArrayOutputStream ) );

        final JVar objectOutput = tryClone.body().decl(
            JMod.FINAL, objectOutputStream, "out", JExpr._new( objectOutputStream ).arg( byteArrayOutput ) );

        tryClone.body().add( objectOutput.invoke( "writeObject" ).arg( s ) );
        tryClone.body().add( objectOutput.invoke( "close" ) );

        final JVar byteArrayInput = tryClone.body().decl(
            JMod.FINAL, byteArrayInputStream, "byteArrayInput",
            JExpr._new( byteArrayInputStream ).arg( byteArrayOutput.invoke( "toByteArray" ) ) );

        final JVar objectInput = tryClone.body().decl(
            JMod.FINAL, objectInputStream, "in", JExpr._new( objectInputStream ).arg( byteArrayInput ) );

        final JVar copy = tryClone.body().decl(
            JMod.FINAL, serializable, "copy", JExpr.cast( serializable, objectInput.invoke( "readObject" ) ) );

        tryClone.body().invoke( objectInput, "close" );
        tryClone.body()._return( copy );

        final JExpression assertionErrorMsg =
            JExpr.lit( "Unexpected instance during copying object '" ).plus( s ).plus( JExpr.lit( "'." ) );

        final JCatchBlock catchSecurityException = tryClone._catch( securityException );
        catchSecurityException.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchSecurityException.param( "e" ) ) ) );

        final JCatchBlock catchClassNotFound = tryClone._catch( classNotFound );
        catchClassNotFound.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchClassNotFound.param( "e" ) ) ) );

        final JCatchBlock catchInvalidClass = tryClone._catch( invalidClass );
        catchInvalidClass.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchInvalidClass.param( "e" ) ) ) );

        final JCatchBlock catchNotSerializable = tryClone._catch( notSerializable );
        catchNotSerializable.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchNotSerializable.param( "e" ) ) ) );

        final JCatchBlock catchStreamCorrupted = tryClone._catch( streamCorrupted );
        catchStreamCorrupted.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchStreamCorrupted.param( "e" ) ) ) );

        final JCatchBlock catchOptionalData = tryClone._catch( optionalData );
        catchOptionalData.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchOptionalData.param( "e" ) ) ) );

        final JCatchBlock catchIOException = tryClone._catch( ioException );
        catchIOException.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchIOException.param( "e" ) ) ) );

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
        final JClass securityException = clazz.parent().getCodeModel().ref( SecurityException.class );
        final JClass illegalArgument = clazz.parent().getCodeModel().ref( IllegalArgumentException.class );
        final JClass initializerError = clazz.parent().getCodeModel().ref( ExceptionInInitializerError.class );
        final JClass assertionError = clazz.parent().getCodeModel().ref( AssertionError.class );
        final JClass classArray = clazz.parent().getCodeModel().ref( Class[].class );
        final JClass objectArray = clazz.parent().getCodeModel().ref( Object[].class );
        final JClass serializable = clazz.parent().getCodeModel().ref( Serializable.class );

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

        m.javadoc().append( "Creates and returns a deep copy of a given object." );
        m.javadoc().addParam( o ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append( "A deep copy of {@code o} or {@code null} if {@code o} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional objectNotNull = m.body()._if( o.ne( JExpr._null() ) );

        final JConditional isPrimitive =
            objectNotNull._then()._if( JExpr.invoke( JExpr.invoke( o, "getClass" ), "isPrimitive" ) );

        isPrimitive._then()._return( o );

        final JConditional isArray =
            objectNotNull._then()._if( JExpr.invoke( JExpr.invoke( o, "getClass" ), "isArray" ) );

        isArray._then()._return( this.getCopyOfArrayInvocation( clazz ).arg( o ) );

        for ( Class immutableType : IMMUTABLE_TYPES )
        {
            objectNotNull._then()._if( o._instanceof( clazz.parent().getCodeModel().ref( immutableType ) ) ).
                _then()._return( o );

        }

        for ( Class cloneableType : CLONEABLE_TYPES )
        {
            final JClass cloneable = clazz.parent().getCodeModel().ref( cloneableType );
            objectNotNull._then()._if( o._instanceof( cloneable ) )._then()._return(
                JExpr.invoke( JExpr.cast( cloneable, o ), ( "clone" ) ) );

        }

        final JConditional instanceOfDOMElement = objectNotNull._then()._if( o._instanceof( element ) );
        instanceOfDOMElement._then()._return( JExpr.cast( element, JExpr.invoke(
            JExpr.cast( element, o ), "cloneNode" ).arg( JExpr.TRUE ) ) );

        final JConditional instanceOfElement = objectNotNull._then()._if( o._instanceof( jaxbElement ) );
        instanceOfElement._then()._return( this.getCopyOfJaxbElementInvocation( clazz ).
            arg( JExpr.cast( jaxbElement, o ) ) );

        final JTryBlock tryCloneMethod = objectNotNull._then()._try();
        tryCloneMethod.body()._return( JExpr.invoke( JExpr.invoke( JExpr.invoke( o, "getClass" ), "getMethod" ).
            arg( "clone" ).arg( JExpr.cast( classArray, JExpr._null() ) ), "invoke" ).arg( o ).
            arg( JExpr.cast( objectArray, JExpr._null() ) ) );

        final JExpression assertionErrorMsg =
            JExpr.lit( "Unexpected instance during copying object '" ).plus( o ).plus( JExpr.lit( "'." ) );

        final JCatchBlock catchNoSuchMethod = tryCloneMethod._catch( noSuchMethod );
        final JConditional instanceOfSerializable = catchNoSuchMethod.body()._if( o._instanceof( serializable ) );
        instanceOfSerializable._then()._return( this.getCopyOfSerializableInvocation( clazz ).
            arg( JExpr.cast( serializable, o ) ) );

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

        final JCatchBlock catchSecurityException = tryCloneMethod._catch( securityException );
        catchSecurityException.body().directStatement( "// Please report this at " +
                                                       this.getMessage( "bugtrackerUrl", null ) );

        catchSecurityException.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchSecurityException.param( "e" ) ) ) );

        final JCatchBlock catchIllegalArgument = tryCloneMethod._catch( illegalArgument );
        catchIllegalArgument.body().directStatement( "// Please report this at " +
                                                     this.getMessage( "bugtrackerUrl", null ) );

        catchIllegalArgument.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchIllegalArgument.param( "e" ) ) ) );

        final JCatchBlock catchInitializerError = tryCloneMethod._catch( initializerError );
        catchInitializerError.body().directStatement( "// Please report this at " +
                                                      this.getMessage( "bugtrackerUrl", null ) );

        catchInitializerError.body()._throw( JExpr.cast( assertionError, JExpr._new( assertionError ).
            arg( assertionErrorMsg ).invoke( "initCause" ).arg( catchInitializerError.param( "e" ) ) ) );

        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE ? clazz._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );
    }

    private JInvocation getCopyOfElementInfoInvocation( final FieldOutline fieldOutline, final CElementInfo element )
    {
        final JType elementType = element.toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION );
        final JType[] signature =
        {
            elementType
        };

        final String methodName;
        if ( element.hasClass() )
        {
            methodName = "copyOf" + element.shortName();
        }
        else
        {
            methodName = "copyOf" + this.getMethodNamePart(
                element.getContentType().toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ) ) + "Element";

        }

        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : fieldOutline.parent()._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return fieldOutline.parent()._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : fieldOutline.parent().implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? fieldOutline.parent()._package().objectFactory().method( JMod.STATIC | mod, elementType, methodName )
              : fieldOutline.parent().implClass.method( JMod.STATIC | mod, elementType, methodName ) );

        final JVar e = m.param( JMod.FINAL, elementType, "e" );

        m.javadoc().append( "Creates and returns a deep copy of a given {@code " + elementType.binaryName() +
                            "} instance." );

        m.javadoc().addParam( e ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append( "A deep copy of {@code e} or {@code null} if {@code e} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional elementNotNull = m.body()._if( e.ne( JExpr._null() ) );

        final JExpression newElement;
        if ( element.hasClass() )
        {
            newElement = JExpr._new( elementType ).arg( this.getCopyExpression(
                fieldOutline, element.getContentType(), elementNotNull._then(),
                JExpr.cast( element.getContentType().toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ),
                            JExpr.invoke( e, "getValue" ) ) ) );

        }
        else
        {
            newElement = JExpr._new( elementType ).
                arg( JExpr.invoke( e, "getName" ) ).
                arg( JExpr.invoke( e, "getDeclaredType" ) ).
                arg( JExpr.invoke( e, "getScope" ) ).
                arg( JExpr.invoke( e, "getValue" ) );

        }

        final JVar copy = elementNotNull._then().decl( JMod.FINAL, elementType, "copy", newElement );
        elementNotNull._then().add( copy.invoke( "setNil" ).arg( e.invoke( "isNil" ) ) );

        if ( !element.hasClass() )
        {
            elementNotNull._then().add( copy.invoke( "setValue" ).arg( this.getCopyExpression(
                fieldOutline, element.getContentType(), elementNotNull._then(),
                JExpr.cast( element.getContentType().toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ),
                            copy.invoke( "getValue" ) ) ) ) );

        }

        elementNotNull._then()._return( copy );
        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE
                 ? fieldOutline.parent()._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );

    }

    private JInvocation getCopyOfArrayInfoInvocation( final FieldOutline fieldOutline, final CArrayInfo array )
    {
        final JType arrayType =
            ( array.getAdapterUse() != null
              ? fieldOutline.parent().parent().getModel().getTypeInfo( array.getAdapterUse().customType ).
            toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION )
              : array.toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ) );

        final JType itemType = array.getItemType().toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION );
        final JType[] signature =
        {
            arrayType
        };

        final String methodName = "copyOf" + fieldOutline.getPropertyInfo().getName( true );
        final int mod = this.getVisibilityModifier();

        if ( mod != JMod.PRIVATE )
        {
            for ( JMethod m : fieldOutline.parent()._package().objectFactory().methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return fieldOutline.parent()._package().objectFactory().staticInvoke( m );
                }
            }
        }
        else
        {
            for ( JMethod m : fieldOutline.parent().implClass.methods() )
            {
                if ( m.name().equals( methodName ) && m.hasSignature( signature ) )
                {
                    return JExpr.invoke( m );
                }
            }
        }

        final JMethod m =
            ( mod != JMod.PRIVATE
              ? fieldOutline.parent()._package().objectFactory().method( JMod.STATIC | mod, arrayType, methodName )
              : fieldOutline.parent().implClass.method( JMod.STATIC | mod, arrayType, methodName ) );

        final JVar a = m.param( JMod.FINAL, arrayType, "array" );

        m.javadoc().append(
            "Creates and returns a deep copy of a given {@code " + arrayType.binaryName() + "} instance." );

        m.javadoc().addParam( a ).append( "The instance to copy or {@code null}." );
        m.javadoc().addReturn().append(
            "A deep copy of {@code array} or {@code null} if {@code array} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional arrayNotNull = m.body()._if( a.ne( JExpr._null() ) );
        final JVar copy = arrayNotNull._then().decl( arrayType, "copy", JExpr.newArray( itemType, a.ref( "length" ) ) );
        final JForLoop forEachItem = arrayNotNull._then()._for();
        final JVar i = forEachItem.init(
            fieldOutline.parent().parent().getCodeModel().INT, "i", a.ref( "length" ).minus( JExpr.lit( 1 ) ) );

        forEachItem.test( i.gte( JExpr.lit( 0 ) ) );
        forEachItem.update( i.decr() );

        final JExpression copyExpr =
            this.getCopyExpression( fieldOutline, array.getItemType(), forEachItem.body(), a.component( i ) );

        forEachItem.body().assign( copy.component( i ), copyExpr );
        arrayNotNull._then()._return( copy );
        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return ( mod != JMod.PRIVATE
                 ? fieldOutline.parent()._package().objectFactory().staticInvoke( m ) : JExpr.invoke( m ) );

    }

    private JMethod getCopyOfPropertyMethod( final FieldOutline field )
    {
        final String methodName = "copy" + field.getPropertyInfo().getName( true );
        for ( JMethod m : field.parent().implClass.methods() )
        {
            if ( m.name().equals( methodName ) )
            {
                return m;
            }
        }

        final JClass jaxbElement = field.parent().parent().getCodeModel().ref( JAXBElement.class );
        final JClass assertionError = field.parent().parent().getCodeModel().ref( AssertionError.class );
        final JMethod m = field.parent().implClass.method(
            this.getVisibilityModifier() | JMod.STATIC, field.getRawType(), methodName );

        final JVar source = m.param( JMod.FINAL, field.getRawType(), "source" );

        m.javadoc().append( "Creates and returns a deep copy of property {@code " +
                            field.getPropertyInfo().getName( true ) + "}." );

        m.javadoc().addParam( source ).append( "The source to copy from or {@code null}." );
        m.javadoc().addReturn().append(
            "A deep copy of {@code source} or {@code null} if {@code source} is {@code null}." );

        m.body().directStatement( "// " + this.getMessage( "title", null ) );

        final JConditional sourceNotNull = m.body()._if( source.ne( JExpr._null() ) );

//        m.body()._if( source.eq( JExpr._null() ) )._then()._throw( JExpr._new( nullPointerException ).arg( "source" ) );
//        m.body()._if( target.eq( JExpr._null() ) )._then()._throw( JExpr._new( nullPointerException ).arg( "target" ) );

        final List<CClassInfo> referencedClassInfos =
            new ArrayList<CClassInfo>( field.getPropertyInfo().ref().size() );

        final List<CElementInfo> referencedElementInfos =
            new ArrayList<CElementInfo>( field.getPropertyInfo().ref().size() );

        final List<CElementInfo> referencedElementInfosWithClass =
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
                if ( e.hasClass() )
                {
                    referencedElementInfosWithClass.add( e );
                }
                else
                {
                    final JType contentType =
                        e.getContentType().toType( field.parent().parent(), Aspect.IMPLEMENTATION );

                    if ( !referencedContentTypes.contains( contentType ) )
                    {
                        referencedContentTypes.add( contentType );
                        referencedElementInfos.add( e );
                    }
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
        Collections.sort( referencedElementInfos, new CElementInfoComparator( field.parent().parent(), false ) );
        Collections.sort( referencedElementInfosWithClass, new CElementInfoComparator( field.parent().parent(), true ) );
        Collections.sort( referencedTypeInfos, new CTypeInfoComparator( field.parent().parent() ) );
        Collections.reverse( referencedClassInfos );
        Collections.reverse( referencedElementInfos );
        Collections.reverse( referencedElementInfosWithClass );
        Collections.reverse( referencedTypeInfos );

        if ( !( referencedElementInfos.isEmpty() && referencedElementInfosWithClass.isEmpty() ) )
        {
            final JBlock elementBlock = sourceNotNull._then()._if( source._instanceof( jaxbElement ) )._then();
            if ( !referencedElementInfosWithClass.isEmpty() )
            {
                elementBlock.directStatement( "// Referenced elements with classes." );
                for ( CElementInfo elementInfo : referencedElementInfosWithClass )
                {
                    final JType elementType = elementInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION );
                    final JConditional ifInstanceOf = elementBlock._if( source._instanceof( elementType ) );
                    final JExpression copyExpr = this.getCopyExpression(
                        field, elementInfo, ifInstanceOf._then(), JExpr.cast( elementType, source ) );

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
                        ifInstanceOf._then()._return( copyExpr );
                    }
                }
            }

            if ( !referencedElementInfos.isEmpty() )
            {
                elementBlock.directStatement( "// Referenced elements without classes." );
                for ( CElementInfo elementInfo : referencedElementInfos )
                {
                    final JType contentType =
                        ( elementInfo.getAdapterUse() != null ? field.parent().parent().getModel().getTypeInfo(
                        elementInfo.getAdapterUse().customType ).toType( field.parent().parent(), Aspect.IMPLEMENTATION )
                          : elementInfo.getContentType().toType( field.parent().parent(), Aspect.IMPLEMENTATION ) );

                    final JConditional ifInstanceOf = elementBlock._if( JExpr.invoke( JExpr.cast(
                        jaxbElement, source ), "getValue" )._instanceof( contentType ) );

                    final JExpression copyExpr = this.getCopyExpression(
                        field, elementInfo, ifInstanceOf._then(), JExpr.cast( jaxbElement, source ) );

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
                        ifInstanceOf._then()._return( copyExpr );
                    }
                }
            }
        }

        for ( CClassInfo classInfo : referencedClassInfos )
        {
            final JType javaType =
                ( classInfo.getAdapterUse() != null ? field.parent().parent().getModel().getTypeInfo(
                classInfo.getAdapterUse().customType ).toType( field.parent().parent(), Aspect.IMPLEMENTATION )
                  : classInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION ) );

            final JConditional ifInstanceOf = sourceNotNull._then()._if( source._instanceof( javaType ) );

            final JExpression copyExpr =
                this.getCopyExpression( field, classInfo, ifInstanceOf._then(), JExpr.cast( javaType, source ) );

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
                ifInstanceOf._then()._return( copyExpr );
            }
        }

        for ( CTypeInfo typeInfo : referencedTypeInfos )
        {
            final JType javaType = typeInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION );
            final JConditional ifInstanceOf = sourceNotNull._then()._if( source._instanceof( javaType ) );
            final JExpression copyExpr =
                this.getCopyExpression( field, typeInfo, ifInstanceOf._then(), JExpr.cast( javaType, source ) );

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
                ifInstanceOf._then()._return( copyExpr );
            }
        }

        sourceNotNull._then().directStatement( "// Please report this at " + this.getMessage( "bugtrackerUrl", null ) );
        sourceNotNull._then()._throw( JExpr._new( assertionError ).arg( JExpr.lit( "Unexpected instance '" ).
            plus( source ).plus( JExpr.lit( "' for property '" + field.getPropertyInfo().getName( true ) +
                                            "' of class '" + field.parent().implClass.binaryName() + "'." ) ) ) );

        m.body()._return( JExpr._null() );
        this.methodCount = this.methodCount.add( BigInteger.ONE );
        return m;
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

        m.javadoc().append( "Copies all values of property {@code " + field.getPropertyInfo().getName( true ) +
                            "} deeply." );

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

        final List<CElementInfo> referencedElementInfosWithClass =
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
                if ( e.hasClass() )
                {
                    referencedElementInfosWithClass.add( e );
                }
                else
                {
                    final JType contentType =
                        e.getContentType().toType( field.parent().parent(), Aspect.IMPLEMENTATION );

                    if ( !referencedContentTypes.contains( contentType ) )
                    {
                        referencedContentTypes.add( contentType );
                        referencedElementInfos.add( e );
                    }
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
        Collections.sort( referencedElementInfos, new CElementInfoComparator( field.parent().parent(), false ) );
        Collections.sort( referencedElementInfosWithClass, new CElementInfoComparator( field.parent().parent(), true ) );
        Collections.sort( referencedTypeInfos, new CTypeInfoComparator( field.parent().parent() ) );
        Collections.reverse( referencedClassInfos );
        Collections.reverse( referencedElementInfos );
        Collections.reverse( referencedElementInfosWithClass );
        Collections.reverse( referencedTypeInfos );

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
            sourceNotEmpty = m.body()._if( JExpr.invoke( source, "isEmpty" ).not() );
            copyLoop = sourceNotEmpty._then()._for();
            it = copyLoop.init( field.parent().parent().getCodeModel().ref( Iterator.class ),
                                "it", source.invoke( "iterator" ) );

            copyLoop.test( JExpr.invoke( it, "hasNext" ) );
            next = copyLoop.body().decl( JMod.FINAL, object, "next", JExpr.invoke( it, "next" ) );
            copy = null;
        }

        if ( !( referencedElementInfos.isEmpty() && referencedElementInfosWithClass.isEmpty() ) )
        {
            final JBlock copyBlock = copyLoop.body()._if( next._instanceof( jaxbElement ) )._then();
            if ( !referencedElementInfosWithClass.isEmpty() )
            {
                copyBlock.directStatement( "// Referenced elements with classes." );
                for ( CElementInfo elementInfo : referencedElementInfosWithClass )
                {
                    final JType elementType = elementInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION );
                    final JConditional ifInstanceOf = copyBlock._if( next._instanceof( elementType ) );
                    final JExpression copyExpr = this.getCopyExpression(
                        field, elementInfo, ifInstanceOf._then(), JExpr.cast( elementType, next ) );

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

                        ifInstanceOf._then()._continue();
                    }
                }
            }

            if ( !referencedElementInfos.isEmpty() )
            {
                copyBlock.directStatement( "// Referenced elements without classes." );
                for ( CElementInfo elementInfo : referencedElementInfos )
                {
                    final JType contentType =
                        ( elementInfo.getAdapterUse() != null ? field.parent().parent().getModel().getTypeInfo(
                        elementInfo.getAdapterUse().customType ).toType( field.parent().parent(), Aspect.IMPLEMENTATION )
                          : elementInfo.getContentType().toType( field.parent().parent(), Aspect.IMPLEMENTATION ) );

                    final JConditional ifInstanceOf = copyBlock._if( JExpr.invoke( JExpr.cast(
                        jaxbElement, next ), "getValue" )._instanceof( contentType ) );

                    final JExpression copyExpr =
                        this.getCopyExpression( field, elementInfo, ifInstanceOf._then(),
                                                JExpr.cast( jaxbElement, next ) );

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
        }

        for ( CClassInfo classInfo : referencedClassInfos )
        {
            final JType javaType =
                ( classInfo.getAdapterUse() != null ? field.parent().parent().getModel().getTypeInfo( classInfo.
                getAdapterUse().customType ).toType( field.parent().parent(), Aspect.IMPLEMENTATION )
                  : classInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION ) );

            final JConditional ifInstanceOf = copyLoop.body()._if( next._instanceof( javaType ) );

            final JExpression copyExpr =
                this.getCopyExpression( field, classInfo, ifInstanceOf._then(), JExpr.cast( javaType, next ) );

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

        for ( CTypeInfo typeInfo : referencedTypeInfos )
        {
            final JType javaType = typeInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION );
            final JConditional ifInstanceOf = copyLoop.body()._if( next._instanceof( javaType ) );
            final JExpression copyExpr =
                this.getCopyExpression( field, typeInfo, ifInstanceOf._then(), JExpr.cast( javaType, next ) );

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

    private JExpression getCopyExpression( final FieldOutline fieldOutline, final CTypeInfo type,
                                           final JBlock block, final JExpression source )
    {
        JExpression expr = null;

        if ( type instanceof CBuiltinLeafInfo )
        {
            expr = this.getBuiltinCopyExpression( fieldOutline, (CBuiltinLeafInfo) type, block, source );
        }
        else if ( type instanceof CWildcardTypeInfo )
        {
            expr = this.getWildcardCopyExpression( fieldOutline, (CWildcardTypeInfo) type, block, source );
        }
        else if ( type instanceof CClassInfo )
        {
            expr = this.getClassInfoCopyExpression( fieldOutline, (CClassInfo) type, block, source );
        }
        else if ( type instanceof CEnumLeafInfo )
        {
            expr = this.getEnumLeafInfoCopyExpression( fieldOutline, (CEnumLeafInfo) type, block, source );
        }
        else if ( type instanceof CArrayInfo )
        {
            expr = this.getArrayCopyExpression( fieldOutline, (CArrayInfo) type, block, source );
        }
        else if ( type instanceof CElementInfo )
        {
            expr = this.getElementCopyExpression( fieldOutline, (CElementInfo) type, block, source );
        }
        else if ( type instanceof CNonElement )
        {
            expr = this.getNonElementCopyExpression( fieldOutline, (CNonElement) type, block, source );
        }

        if ( expr != null )
        {
            this.expressionCount = this.expressionCount.add( BigInteger.ONE );
        }

        return expr;
    }

    private JExpression getBuiltinCopyExpression( final FieldOutline fieldOutline, final CBuiltinLeafInfo type,
                                                  final JBlock block, final JExpression source )
    {
        JExpression expr = null;

        block.directStatement( "// CBuiltinLeafInfo: " +
                               type.toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ).binaryName() );

        if ( type == CBuiltinLeafInfo.ANYTYPE )
        {
            expr = this.getCopyOfObjectInvocation( fieldOutline.parent() ).arg( source );
        }
        else if ( type == CBuiltinLeafInfo.BASE64_BYTE_ARRAY )
        {
            final JClass byteArray = fieldOutline.parent().parent().getCodeModel().ref( byte[].class );
            expr = this.getCopyOfPrimitiveArrayExpression( fieldOutline.parent(), byteArray, source );
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
            expr = source;
        }
        else if ( type == CBuiltinLeafInfo.CALENDAR )
        {
            final JClass xmlCal = fieldOutline.parent().parent().getCodeModel().ref( XMLGregorianCalendar.class );
            expr = JOp.cond( source.eq( JExpr._null() ), JExpr._null(),
                             JExpr.cast( xmlCal, source.invoke( "clone" ) ) );

        }
        else if ( type == CBuiltinLeafInfo.DURATION )
        {
            expr = source;
        }
        else if ( type == CBuiltinLeafInfo.DATA_HANDLER || type == CBuiltinLeafInfo.IMAGE ||
                  type == CBuiltinLeafInfo.XML_SOURCE )
        {
            this.log( Level.WARNING, "cannotCopyType", new Object[]
                {
                    type.toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ).fullName(),
                    fieldOutline.getPropertyInfo().getName( true ), fieldOutline.parent().implClass.fullName()
                } );

            expr = source;
        }

        return expr;
    }

    private JExpression getWildcardCopyExpression( final FieldOutline fieldOutline, final CWildcardTypeInfo type,
                                                   final JBlock block, final JExpression source )
    {
        block.directStatement( "// CWildcardTypeInfo: " +
                               type.toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return JOp.cond( source.eq( JExpr._null() ), JExpr._null(),
                         JExpr.cast( fieldOutline.parent().parent().getCodeModel().ref( Element.class ),
                                     source.invoke( "cloneNode" ).arg( JExpr.TRUE ) ) );

    }

    private JExpression getClassInfoCopyExpression( final FieldOutline fieldOutline, final CClassInfo type,
                                                    final JBlock block, final JExpression source )
    {
        block.directStatement(
            "// CClassInfo: " + type.toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return JOp.cond( source.eq( JExpr._null() ), JExpr._null(), source.invoke( "clone" ) );
    }

    private JExpression getNonElementCopyExpression( final FieldOutline fieldOutline, final CNonElement type,
                                                     final JBlock block, final JExpression source )
    {
        block.directStatement(
            "// CNonElement: " + type.toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return null;
    }

    private JExpression getArrayCopyExpression( final FieldOutline fieldOutline, final CArrayInfo type,
                                                final JBlock block, final JExpression source )
    {
        block.directStatement( "// CArrayInfo: " + type.fullName() );
        return this.getCopyOfArrayInfoInvocation( fieldOutline, type ).arg( source );
    }

    private JExpression getElementCopyExpression( final FieldOutline fieldOutline, final CElementInfo type,
                                                  final JBlock block, final JExpression source )
    {
        block.directStatement(
            "// CElementInfo: " + type.toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return this.getCopyOfElementInfoInvocation( fieldOutline, type ).arg( source );
    }

    private JExpression getEnumLeafInfoCopyExpression( final FieldOutline fieldOutline, final CEnumLeafInfo type,
                                                       final JBlock block, final JExpression source )
    {
        block.directStatement(
            "// CEnumLeafInfo: " + type.toType( fieldOutline.parent().parent(), Aspect.IMPLEMENTATION ).binaryName() );

        return source;
    }

    private JMethod generateStandardConstructor( final ClassOutline clazz )
    {
        final JMethod ctor = clazz.implClass.constructor( JMod.PUBLIC );
        ctor.body().directStatement( " // " + this.getMessage( "title", null ) );
        ctor.body().invoke( "super" );
        ctor.javadoc().add( "Creates a new {@code " + clazz.implClass.name() + "} instance." );
        this.constructorCount = this.constructorCount.add( BigInteger.ONE );
        return ctor;
    }

    private JMethod generateCopyConstructor( final ClassOutline clazz )
    {
        final JMethod ctor = clazz.implClass.constructor( JMod.PUBLIC );
        final JVar o = ctor.param( JMod.FINAL, clazz.implClass, "o" );

        ctor.javadoc().add( "Creates a new {@code " + clazz.implClass.name() +
                            "} instance by deeply copying a given instance." );

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
                        this.log( Level.WARNING, "fieldWithoutProperties", new Object[]
                            {
                                field.name(), clazz.implClass.name()
                            } );

                    }
                    else
                    {
                        if ( field.name().equals( "otherAttributes" ) && clazz.target.declaresAttributeWildcard() )
                        {
                            paramNotNullBlock.add(
                                JExpr.refthis( field.name() ).invoke( "putAll" ).arg( o.ref( field ) ) );

                        }
                        else
                        {
                            paramNotNullBlock.assign( JExpr.refthis( field.name() ), JExpr.cast(
                                field.type(), this.getCopyOfObjectInvocation( clazz ).arg( o.ref( field ) ) ) );

                            this.log( Level.WARNING, "fieldWithoutProperties", new Object[]
                                {
                                    field.name(), clazz.implClass.name()
                                } );

                        }
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
        cloneMethod.javadoc().append( "Creates and returns a deep copy of this object.\n" );
        cloneMethod.javadoc().addReturn().append( "A deep copy of this object." );
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
                final JExpression copyExpr;
                if ( field.getPropertyInfo().ref().size() != 1 )
                {
                    block.directStatement( "// '" + field.getPropertyInfo().getName( true ) + "' property." );
                    copyExpr = JExpr.invoke( this.getCopyOfPropertyMethod( field ) ).arg( o.invoke( getter ) );
                }
                else
                {
                    final CTypeInfo typeInfo =
                        ( field.getPropertyInfo().getAdapter() != null ? field.parent().parent().getModel().getTypeInfo(
                        field.getPropertyInfo().getAdapter().customType ) : field.getPropertyInfo().ref().iterator().
                        next() );

                    final JType javaType = typeInfo.toType( field.parent().parent(), Aspect.IMPLEMENTATION );
                    copyExpr = this.getCopyExpression( field, typeInfo, block,
                                                       JExpr.cast( javaType, JExpr.invoke( o, getter ) ) );

                }

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

        methodName = methodName.replace( ".", "" );
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

        int logLevel = Level.WARNING.intValue();
        if ( this.options != null && !this.options.quiet )
        {
            if ( this.options.verbose )
            {
                logLevel = Level.INFO.intValue();
            }
            if ( this.options.debugMode )
            {
                logLevel = Level.ALL.intValue();
            }
        }

        if ( level.intValue() >= logLevel )
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

class CClassInfoComparator implements Comparator<CClassInfo>
{

    private final Outline outline;

    CClassInfoComparator( final Outline outline )
    {
        this.outline = outline;
    }

    public int compare( final CClassInfo o1, final CClassInfo o2 )
    {
        final JClass javaClass1 = o1.toType( this.outline, Aspect.IMPLEMENTATION );
        final JClass javaClass2 = o2.toType( this.outline, Aspect.IMPLEMENTATION );

        int ret = 0;

        if ( !javaClass1.binaryName().equals( javaClass2.binaryName() ) )
        {
            if ( javaClass1.isAssignableFrom( javaClass2 ) )
            {
                ret = -1;
            }
            else if ( javaClass2.isAssignableFrom( javaClass1 ) )
            {
                ret = 1;
            }
        }

        return ret;
    }

}

class CElementInfoComparator implements Comparator<CElementInfo>
{

    private final Outline outline;

    private final boolean hasClass;

    CElementInfoComparator( final Outline outline, final boolean hasClass )
    {
        this.outline = outline;
        this.hasClass = hasClass;
    }

    public int compare( final CElementInfo o1, final CElementInfo o2 )
    {
        final JClass javaClass1;
        final JClass javaClass2;

        if ( this.hasClass )
        {
            javaClass1 = (JClass) o1.toType( this.outline, Aspect.IMPLEMENTATION );
            javaClass2 = (JClass) o2.toType( this.outline, Aspect.IMPLEMENTATION );
        }
        else
        {
            javaClass1 = (JClass) o1.getContentType().toType( this.outline, Aspect.IMPLEMENTATION );
            javaClass2 = (JClass) o2.getContentType().toType( this.outline, Aspect.IMPLEMENTATION );
        }

        int ret = 0;

        if ( !javaClass1.binaryName().equals( javaClass2.binaryName() ) )
        {
            if ( javaClass1.isAssignableFrom( javaClass2 ) )
            {
                ret = -1;
            }
            else if ( javaClass2.isAssignableFrom( javaClass1 ) )
            {
                ret = 1;
            }
        }

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

    public int compare( final CTypeInfo o1, final CTypeInfo o2 )
    {
        final JType javaType1 = o1.toType( this.outline, Aspect.IMPLEMENTATION );
        final JType javaType2 = o2.toType( this.outline, Aspect.IMPLEMENTATION );

        int ret = 0;

        if ( !javaType1.binaryName().equals( javaType2.binaryName() ) &&
             javaType1 instanceof JClass && javaType2 instanceof JClass )
        {
            if ( ( (JClass) javaType1 ).isAssignableFrom( (JClass) javaType2 ) )
            {
                ret = -1;
            }
            else if ( ( (JClass) javaType2 ).isAssignableFrom( (JClass) javaType1 ) )
            {
                ret = 1;
            }
        }

        return ret;
    }

}
