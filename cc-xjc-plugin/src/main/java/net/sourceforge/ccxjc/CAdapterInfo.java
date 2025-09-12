package net.sourceforge.ccxjc;

import com.sun.codemodel.JExpression;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.model.CAdapter;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.model.nav.NType;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSComponent;
import com.sun.xml.xsom.XmlString;
import jakarta.activation.MimeType;
import org.glassfish.jaxb.core.v2.model.annotation.Locatable;
import org.glassfish.jaxb.core.v2.model.core.ID;
import org.glassfish.jaxb.core.v2.runtime.Location;
import org.xml.sax.Locator;

import javax.xml.namespace.QName;

class CAdapterInfo implements CTypeInfo {

    private final CAdapter adapter;

    CAdapterInfo(final CAdapter adapter) {
        this.adapter = adapter;
    }

    public JType toType(final Outline o, final Aspect aspect) {
        return this.adapter.customType.toType(o, aspect);
    }

    public NType getType() {
        return this.adapter.customType;
    }

    public boolean canBeReferencedByIDREF() {
        return false;
    }

    public Locatable getUpstream() {
        return null;
    }

    public Location getLocation() {
        return null;
    }

    public CCustomizations getCustomizations() {
        return null;
    }

    public Locator getLocator() {
        return null;
    }

    public XSComponent getSchemaComponent() {
        return null;
    }

    public QName getTypeName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isSimpleType() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isCollection() {
        return false;
    }

    public CAdapter getAdapterUse() {
        return this.adapter;
    }

    public CTypeInfo getInfo() {
        return this;
    }

    public ID idUse() {
        return null;
    }

    public MimeType getExpectedMimeType() {
        return null;
    }

    public JExpression createConstant(Outline outline, XmlString lexical) {
        return null;
    }

}
