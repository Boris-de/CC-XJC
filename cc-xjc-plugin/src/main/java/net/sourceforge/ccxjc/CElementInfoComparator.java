package net.sourceforge.ccxjc;

import com.sun.codemodel.JClass;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.Outline;

import java.util.Comparator;

class CElementInfoComparator implements Comparator<CElementInfo> {

    private final Outline outline;

    private final boolean hasClass;

    CElementInfoComparator(final Outline outline, final boolean hasClass) {
        this.outline = outline;
        this.hasClass = hasClass;
    }

    public int compare(final CElementInfo o1, final CElementInfo o2) {
        final JClass javaClass1;
        final JClass javaClass2;

        if (this.hasClass) {
            javaClass1 = (JClass) o1.toType(this.outline, Aspect.IMPLEMENTATION);
            javaClass2 = (JClass) o2.toType(this.outline, Aspect.IMPLEMENTATION);
        } else {
            javaClass1 = (JClass) o1.getContentType().toType(this.outline, Aspect.IMPLEMENTATION);
            javaClass2 = (JClass) o2.getContentType().toType(this.outline, Aspect.IMPLEMENTATION);
        }

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
