package net.sourceforge.ccxjc;

import com.sun.codemodel.JType;
import com.sun.tools.xjc.model.CAdapter;
import com.sun.tools.xjc.model.CCustomizations;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.model.nav.NType;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.Outline;
import com.sun.xml.xsom.XSComponent;
import org.glassfish.jaxb.core.v2.model.annotation.Locatable;
import org.glassfish.jaxb.core.v2.runtime.Location;
import org.xml.sax.Locator;

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
}
