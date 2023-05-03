/*
 * Copyright (C) 2009 The CC-XJC Project. All rights reserved.
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
 */
package net.sourceforge.ccxjc.it;

import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import net.sourceforge.ccxjc.it.model.priv.indexed.valueclass.ccxjcit.SimpleTypeAttributes;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@code SimpleTypeAttributes} complex type.
 *
 * @author <a href="mailto:cs@schulte.it">Christian Schulte</a>
 */
public class SimpleTypeAttributesIndexedTest
{

    private static final String CCXJC_NS = "http://sourceforge.net/ccxjc/it";

    private XMLGregorianCalendar testCalendar;

    private Duration testDuration;

    private byte[] testBytes;

    private String[] testEntities;

    private Object[] testIdRefs;

    private String[] testTokens;

    private QName testQName;

    public XMLGregorianCalendar getTestCalendar() throws DatatypeConfigurationException
    {
        if ( this.testCalendar == null )
        {
            this.testCalendar = CommonHelper.now();
        }

        return this.testCalendar;
    }

    public Duration getTestDuration() throws DatatypeConfigurationException
    {
        if ( this.testDuration == null )
        {
            this.testDuration = CommonHelper.createDurationFromMillis( 1000L );
        }

        return this.testDuration;
    }

    public byte[] getTestBytes()
    {
        if ( this.testBytes == null )
        {
            this.testBytes = new byte[ 256 ];
            for ( int i = 255; i >= 0; i-- )
            {
                this.testBytes[i] = (byte) i;
            }
        }

        return this.testBytes;
    }

    public String[] getTestEntities()
    {
        if ( this.testEntities == null )
        {
            this.testEntities = new String[]
                {
                    "ENTITY 1", "ENTITY 2", "ENTITY 3"
                };

        }

        return this.testEntities;
    }

    public Object[] getTestIdRefs()
    {
        if ( this.testIdRefs == null )
        {
            this.testIdRefs = new Object[]
                {
                    "ID"
                };

        }

        return this.testIdRefs;
    }

    public String[] getTestTokens()
    {
        if ( this.testTokens == null )
        {
            this.testTokens = new String[]
                {
                    "NMTOKEN 1", "NMTOKEN 2", "NMTOKEN 3"
                };

        }

        return this.testTokens;
    }

    public QName getTestQName()
    {
        if ( this.testQName == null )
        {
            this.testQName = new QName( CCXJC_NS, "ID" );
        }

        return this.testQName;
    }

    public void assertTestBytes( final byte[] bytes )
    {
        Assertions.assertArrayEquals( this.getTestBytes(), bytes );
    }

    public void assertTestEntities( final String[] entities )
    {
        Assertions.assertArrayEquals( this.getTestEntities(), entities );
    }

    public void assertTestIdRefs( final Object[] idrefs )
    {
        Assertions.assertArrayEquals( this.getTestIdRefs(), idrefs );
    }

    public void assertTestTokens( final String[] tokens )
    {
        Assertions.assertArrayEquals( this.getTestTokens(), tokens );
    }

    public SimpleTypeAttributes getTestSimpleTypeAttributes() throws DatatypeConfigurationException
    {
        final SimpleTypeAttributes t = new SimpleTypeAttributes();

        t.setAnySimpleType( "any" );
        t.setAnyURI( "anyURI" );
        t.setBase64Binary( this.getTestBytes() );
        t.setBoolean( true );
        t.setByte( (byte) 1 );
        t.setDate( this.getTestCalendar() );
        t.setDateTime( this.getTestCalendar() );
        t.setDecimal( BigDecimal.TEN );
        t.setDouble( 100.0D );
        t.setDuration( this.getTestDuration() );
        t.setENTITY( "ENTITY" );
        t.setFloat( 100.0F );
        t.setGDay( this.getTestCalendar() );
        t.setGMonth( this.getTestCalendar() );
        t.setGMonthDay( this.getTestCalendar() );
        t.setGYear( this.getTestCalendar() );
        t.setGYearMonth( this.getTestCalendar() );
        t.setHexBinary( this.getTestBytes() );
        t.setID( "ID" );
        t.setIDREF( "ID" );
        t.setInt( 100 );
        t.setInteger( BigInteger.TEN );
        t.setLanguage( "en" );
        t.setLong( 100L );
        t.setNCName( "NCName" );
        t.setNMTOKEN( "NMTOKEN" );
        t.setNOTATION( this.getTestQName() );
        t.setName( "name" );
        t.setNegativeInteger( BigInteger.valueOf( -100L ) );
        t.setNonNegativeInteger( BigInteger.TEN );
        t.setNonPositiveInteger( BigInteger.valueOf( -100L ) );
        t.setNormalizedString( "normalized" );
        t.setPositiveInteger( BigInteger.TEN );
        t.setQName( this.getTestQName() );
        t.setShort( (short) 100 );
        t.setString( "String" );
        t.setTime( this.getTestCalendar() );
        t.setToken( "Token" );
        t.setUnsignedByte( (short) 100 );
        t.setUnsignedInt( 100L );
        t.setUnsignedLong( BigInteger.TEN );
        t.setUnsignedShort( 100 );

        t.setENTITIES( this.getTestEntities() );
        t.setIDREFS( this.getTestIdRefs() );
        t.setNMTOKENS( this.getTestTokens() );

        return t;
    }

    public void assertTestSimpleTypeAttributes( final SimpleTypeAttributes a ) throws DatatypeConfigurationException
    {
        Assertions.assertEquals( "any", a.getAnySimpleType() );
        Assertions.assertEquals( "anyURI", a.getAnyURI() );
        this.assertTestBytes( a.getBase64Binary() );
        Assertions.assertTrue( a.isBoolean() );
        Assertions.assertEquals( 1, a.getByte() );
        Assertions.assertEquals( this.getTestCalendar(), a.getDate() );
        Assertions.assertEquals( this.getTestCalendar(), a.getDateTime() );
        Assertions.assertEquals( BigDecimal.TEN, a.getDecimal() );
        Assertions.assertEquals(100.0D, a.getDouble(), CommonHelper.DOUBLE_EPSILON );
        Assertions.assertEquals( this.getTestDuration(), a.getDuration() );
        this.assertTestEntities( a.getENTITIES() );
        Assertions.assertEquals( "ENTITY", a.getENTITY() );
        Assertions.assertEquals(100.0F, a.getFloat(), CommonHelper.FLOAT_EPSILON );
        Assertions.assertEquals( this.getTestCalendar(), a.getGDay() );
        Assertions.assertEquals( this.getTestCalendar(), a.getGMonth() );
        Assertions.assertEquals( this.getTestCalendar(), a.getGMonthDay() );
        Assertions.assertEquals( this.getTestCalendar(), a.getGYear() );
        Assertions.assertEquals( this.getTestCalendar(), a.getGYearMonth() );
        this.assertTestBytes( a.getHexBinary() );
        Assertions.assertEquals( "ID", a.getID() );
        Assertions.assertEquals( "ID", a.getIDREF() );
        this.assertTestIdRefs( a.getIDREFS() );
        Assertions.assertEquals( 100, a.getInt() );
        Assertions.assertEquals( BigInteger.TEN, a.getInteger() );
        Assertions.assertEquals( "en", a.getLanguage() );
        Assertions.assertEquals( 100L, a.getLong() );
        Assertions.assertEquals( "NCName", a.getNCName() );
        Assertions.assertEquals( "NMTOKEN", a.getNMTOKEN() );
        this.assertTestTokens( a.getNMTOKENS() );
        Assertions.assertEquals( this.getTestQName(), a.getNOTATION() );
        Assertions.assertEquals( "name", a.getName() );
        Assertions.assertEquals( BigInteger.valueOf( -100L ), a.getNegativeInteger() );
        Assertions.assertEquals( BigInteger.TEN, a.getNonNegativeInteger() );
        Assertions.assertEquals( BigInteger.valueOf( -100L ), a.getNonPositiveInteger() );
        Assertions.assertEquals( "normalized", a.getNormalizedString() );
        Assertions.assertEquals( BigInteger.TEN, a.getPositiveInteger() );
        Assertions.assertEquals( this.getTestQName(), a.getQName() );
        Assertions.assertEquals( 100, a.getShort() );
        Assertions.assertEquals( "String", a.getString() );
        Assertions.assertEquals( this.getTestCalendar(), a.getTime() );
        Assertions.assertEquals( "Token", a.getToken() );
        Assertions.assertEquals( 100, a.getUnsignedByte() );
        Assertions.assertEquals( 100, a.getUnsignedInt() );
        Assertions.assertEquals( BigInteger.TEN, a.getUnsignedLong() );
        Assertions.assertEquals( 100, a.getUnsignedShort() );
    }

    @Test public void testSimpleTypeAttributesNull() throws Exception
    {
        new SimpleTypeAttributes( null );
    }

    @Test public void testSimpleTypeAttributesDefaults() throws Exception
    {
        new SimpleTypeAttributes( new SimpleTypeAttributes() );
    }

    @Test public void testSimpleTypeAttributes() throws Exception
    {
        this.assertTestSimpleTypeAttributes( new SimpleTypeAttributes( this.getTestSimpleTypeAttributes() ) );
    }

    @Test public void testSerializable() throws Exception
    {
        final int runs = 100000;

        System.gc();
        System.gc();
        System.gc();

        final SimpleTypeAttributes a = this.getTestSimpleTypeAttributes();
        long start = System.currentTimeMillis();
        for ( int i = runs; i >= 0; i-- )
        {
            this.assertTestSimpleTypeAttributes( (SimpleTypeAttributes) SerializationUtils.clone( a ) );
        }
        final long serializableMillis = System.currentTimeMillis() - start;

        System.gc();
        System.gc();
        System.gc();

        /*

        java.lang.ClassCastException: [Ljava.lang.Object;
        at net.sourceforge.ccxjc.it.model.priv.indexed.valueclass.ccxjcit.SimpleTypeAttributes.copyTo(SimpleTypeAttributes.java:2463)
        at net.sourceforge.ccxjc.it.model.priv.indexed.valueclass.ccxjcit.SimpleTypeAttributes.copyTo(SimpleTypeAttributes.java:2483)
        at net.sourceforge.ccxjc.it.SimpleTypeAttributesIndexedTest.testSerializable(SimpleTypeAttributesIndexedTest.java:316)

        start = System.currentTimeMillis();
        for ( int i = runs; i >= 0; i-- )
        {
        this.assertTestSimpleTypeAttributes( (SimpleTypeAttributes) a.copyTo( null ) );
        }
        final long jaxbMillis = System.currentTimeMillis() - start;

        System.gc();
        System.gc();
        System.gc();

         */

        start = System.currentTimeMillis();
        for ( int i = runs; i >= 0; i-- )
        {
            this.assertTestSimpleTypeAttributes( new SimpleTypeAttributes( a ) );
        }
        final long copyMillis = System.currentTimeMillis() - start;

        System.gc();
        System.gc();
        System.gc();

        System.out.println( "Creating " + runs + " copies using serialization took " + serializableMillis +
                            "ms. (100%)" );

//        System.out.println( "Creating " + runs + " copies using jaxb2_commons took " + jaxbMillis + "ms. (" +
//                            ( 100L * jaxbMillis / serializableMillis ) + "%)" );

        System.out.println( "Creating " + runs + " copies using copy constructor took " + copyMillis + "ms. (" +
                            ( 100L * copyMillis / serializableMillis ) + "%)" );

        Assertions.assertTrue( copyMillis < serializableMillis );
    }

}
