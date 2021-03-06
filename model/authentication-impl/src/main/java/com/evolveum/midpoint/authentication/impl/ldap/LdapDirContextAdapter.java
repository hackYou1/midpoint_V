/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.ldap;

import org.springframework.ldap.core.DirContextAdapter;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Define focus during processing of Ldap authentication module.
 *
 * @author skublik
 */

public class LdapDirContextAdapter extends DirContextAdapter {

    public LdapDirContextAdapter(DirContextAdapter dirContextAdapter){
        super(dirContextAdapter);
    }

    private String namingAttr;

    private Class<? extends FocusType> focusType = UserType.class;

    public void setNamingAttr(String namingAttr) {
        this.namingAttr = namingAttr;
    }

    public String getNamingAttr() {
        return namingAttr;
    }

    public void setFocusType(Class<? extends FocusType> focusType) {
        this.focusType = focusType;
    }

    public Class<? extends FocusType> getFocusType() {
        return focusType;
    }
}
