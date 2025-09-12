package net.sourceforge.ccxjc;

import com.sun.codemodel.JClass;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.Outline;

import java.util.Comparator;

class CClassInfoComparator implements Comparator<CClassInfo> {

    private final Outline outline;

    CClassInfoComparator(final Outline outline) {
        this.outline = outline;
    }

    public int compare(final CClassInfo o1, final CClassInfo o2) {
        final JClass javaClass1 = o1.toType(this.outline, Aspect.IMPLEMENTATION);
        final JClass javaClass2 = o2.toType(this.outline, Aspect.IMPLEMENTATION);

        int ret = 0;

        if (!javaClass1.binaryName().equals(javaClass2.binaryName())) {
            if (javaClass1.isAssignableFrom(javaClass2)) {
                ret = -1;
            } else if (javaClass2.isAssignableFrom(javaClass1)) {
                ret = 1;
            }
        }

        return ret;
    }

}
