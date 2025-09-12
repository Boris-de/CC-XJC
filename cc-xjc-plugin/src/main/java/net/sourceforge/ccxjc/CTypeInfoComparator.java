package net.sourceforge.ccxjc;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.model.CTypeInfo;
import com.sun.tools.xjc.outline.Aspect;
import com.sun.tools.xjc.outline.Outline;

import java.util.Comparator;

class CTypeInfoComparator implements Comparator<CTypeInfo> {

    private final Outline outline;

    CTypeInfoComparator(final Outline outline) {
        this.outline = outline;
    }

    public int compare(final CTypeInfo o1, final CTypeInfo o2) {
        final JType javaType1 = o1.toType(this.outline, Aspect.IMPLEMENTATION);
        final JType javaType2 = o2.toType(this.outline, Aspect.IMPLEMENTATION);

        int ret = 0;

        if (!javaType1.binaryName().equals(javaType2.binaryName()) && javaType1 instanceof JClass
                && javaType2 instanceof JClass) {
            if (((JClass) javaType1).isAssignableFrom((JClass) javaType2)) {
                ret = -1;
            } else if (((JClass) javaType2).isAssignableFrom((JClass) javaType1)) {
                ret = 1;
            }
        }

        return ret;
    }

}
