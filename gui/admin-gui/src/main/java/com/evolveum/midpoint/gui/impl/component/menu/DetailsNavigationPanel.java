/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.menu;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.ObjectDetailsStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

public class DetailsNavigationPanel<O extends ObjectType> extends BasePanel<List<ContainerPanelConfigurationType>> {

    private static final String ID_NAV = "menu";
    private static final String ID_NAV_ITEM = "navItem";
    private static final String ID_NAV_ITEM_LINK = "navItemLink";
    private static final String ID_NAV_ITEM_ICON = "navItemIcon";
    private static final String ID_SUB_NAVIGATION = "subNavigation";
    private static final String ID_COUNT = "count";
    private static final String ID_NAVIGATION_DETAILS = "navLinkStyle";

    private ObjectDetailsModels<O> objectDetialsModel;

    public DetailsNavigationPanel(String id, ObjectDetailsModels<O> objectDetialsModel, IModel<List<ContainerPanelConfigurationType>> model) {
        super(id, model);
        this.objectDetialsModel = objectDetialsModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        ListView<ContainerPanelConfigurationType> listView = new ListView<>(ID_NAV, getModel()) {

            @Override
            protected void populateItem(ListItem<ContainerPanelConfigurationType> item) {
                WebMarkupContainer navigationDetails = new WebMarkupContainer(ID_NAVIGATION_DETAILS);
                navigationDetails.add(AttributeAppender.append("class", new ReadOnlyModel<>(() -> {
                    ObjectDetailsStorage storage = getPageBase().getSessionStorage().getObjectDetailsStorage("details" + objectDetialsModel.getObjectWrapperModel().getObject().getCompileTimeClass().getSimpleName());
                    ContainerPanelConfigurationType storageConfig = storage.getDefaultConfiguration();
                    ContainerPanelConfigurationType itemModelObject = item.getModelObject();
                    if (storageConfig.getIdentifier() != null && storageConfig.getIdentifier().equals(itemModelObject.getIdentifier())) {
                        return "active";
                    }
                    return "";
                })));
                item.add(navigationDetails);
                AjaxLink<Void> link = new AjaxLink<>(ID_NAV_ITEM_LINK) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        target.add(DetailsNavigationPanel.this);
                        onClickPerformed(item.getModelObject(), target);
                    }
                };
                navigationDetails.add(link);
                WebMarkupContainer icon = new WebMarkupContainer(ID_NAV_ITEM_ICON);
                icon.setOutputMarkupId(true);
                icon.add(AttributeAppender.append("class", getMenuItemIconClass(item.getModelObject())));
                link.add(icon);
                Label buttonLabel = new Label(ID_NAV_ITEM, Model.of(createButtonLabel(item.getModelObject())));
                link.add(buttonLabel);

                IModel<String> countModel = createCountModel(item.getModel());
                Label label = new Label(ID_COUNT, countModel);
                label.add(new VisibleBehaviour(() -> countModel.getObject() != null));
                link.add(label);

                DetailsNavigationPanel subPanel = new DetailsNavigationPanel(ID_SUB_NAVIGATION, objectDetialsModel, new PropertyModel<>(item.getModel(), ContainerPanelConfigurationType.F_PANEL.getLocalPart())) {

                    @Override
                    protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {
                        if (config.getPath() == null) {
                            config.setPath(item.getModelObject().getPath());
                        }
                        target.add(DetailsNavigationPanel.this);
                        DetailsNavigationPanel.this.onClickPerformed(config, target);
                    }
                };
                subPanel.add(new VisibleBehaviour(() -> !item.getModelObject().getPanel().isEmpty()));
                navigationDetails.add(subPanel);
                item.add(new VisibleBehaviour(() -> isMenuItemVisible(item.getModelObject())));

//                item.add(new Label(ID_NAV_ITEM, item.getModel()));o
            }
        };
        listView.setOutputMarkupId(true);
        add(listView);
    }

    private IModel<String> createCountModel(IModel<ContainerPanelConfigurationType> panelModel) {
        return new ReadOnlyModel<>( () -> {
            ContainerPanelConfigurationType config = panelModel.getObject();
            String panelInstanceIdentifier = config.getIdentifier();

            SimpleCounter counter = getPageBase().getCounterProvider(panelInstanceIdentifier);
            if (counter == null || counter.getClass().equals(SimpleCounter.class)) {
                return null;
            }

            int count = counter.count(objectDetialsModel, getPageBase());
//            if (count == 0) {
//                return null;
//            }
            return String.valueOf(count);
        });
    }

    private boolean isMenuItemVisible(ContainerPanelConfigurationType config) {
        if (config == null) {
            return true;
        }

        UserInterfaceElementVisibilityType visibility = config.getVisibility();
        if (visibility == null) {
            return true;
        }

        if (UserInterfaceElementVisibilityType.HIDDEN == visibility) {
            return false;
        }

        return true;
    }

    protected void onClickPerformed(ContainerPanelConfigurationType config, AjaxRequestTarget target) {

    }

    private String createButtonLabel(ContainerPanelConfigurationType config) {
        if (config.getDisplay() == null) {
            return "N/A";
        }

        if (config.getDisplay().getLabel() == null) {
            return "N/A";
        }

        return config.getDisplay().getLabel().getOrig();
    }

    private String getMenuItemIconClass(ContainerPanelConfigurationType item) {
        if (item == null || item.getDisplay() == null) {
            return GuiStyleConstants.CLASS_CIRCLE_FULL;
        }
        String iconCss = WebComponentUtil.getIconCssClass(item.getDisplay());
        return StringUtils.isNoneEmpty(iconCss) ? iconCss : GuiStyleConstants.CLASS_CIRCLE_FULL;
    }

}