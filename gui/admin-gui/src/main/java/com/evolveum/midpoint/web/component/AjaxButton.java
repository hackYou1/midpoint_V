/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public abstract class AjaxButton extends AjaxLink<String> {

    public AjaxButton(String id) {
        super(id);
    }

    public AjaxButton(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        String text = getModelObject();
        if (StringUtils.isNotEmpty(text)) {
            replaceComponentTagBody(markupStream, openTag, text);
            return;
        }

        super.onComponentTagBody(markupStream, openTag);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (tag.isOpenClose()) {
            tag.setType(XmlTag.TagType.OPEN);
        }
    }

    @Override
    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
        attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
    }

}
