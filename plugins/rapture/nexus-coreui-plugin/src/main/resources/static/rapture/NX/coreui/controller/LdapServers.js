/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-2015 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
/*global Ext, NX*/

/**
 * LDAP controller.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.controller.LdapServers', {
  extend: 'NX.controller.Drilldown',
  requires: [
    'NX.Conditions',
    'NX.Messages',
    'NX.Permissions',
    'NX.I18n'
  ],

  masters: 'nx-coreui-ldapserver-list',

  models: [
    'LdapServer'
  ],
  stores: [
    'LdapServer',
    'LdapSchemaTemplate'
  ],
  views: [
    'ldap.LdapServerAdd',
    'ldap.LdapServerChangeOrder',
    'ldap.LdapServerFeature',
    'ldap.LdapServerList',
    'ldap.LdapServerBackup',
    'ldap.LdapServerBackupFieldSet',
    'ldap.LdapServerBackupForm',
    'ldap.LdapServerConnection',
    'ldap.LdapServerConnectionFieldSet',
    'ldap.LdapServerConnectionForm',
    'ldap.LdapServerUserAndGroup',
    'ldap.LdapServerUserAndGroupFieldSet',
    'ldap.LdapServerUserAndGroupForm',
    'ldap.LdapServerUserAndGroupLoginCredentials',
    'ldap.LdapServerUserAndGroupMappingTestResults'
  ],
  refs: [
    { ref: 'feature', selector: 'nx-coreui-ldapserver-feature' },
    { ref: 'list', selector: 'nx-coreui-ldapserver-list' },
    { ref: 'connection', selector: 'nx-coreui-ldapserver-feature nx-coreui-ldapserver-connection' },
    { ref: 'backup', selector: 'nx-coreui-ldapserver-feature nx-coreui-ldapserver-backup' },
    { ref: 'userAndGroup', selector: 'nx-coreui-ldapserver-feature nx-coreui-ldapserver-userandgroup' }
  ],
  icons: {
    'ldapserver-default': {
      file: 'book_addresses.png',
      variants: ['x16', 'x32']
    },
    'ldap-security-source': {
      file: 'book_addresses.png',
      variants: ['x16']
    }
  },
  features: {
    mode: 'admin',
    path: '/Security/LDAP',
    text: NX.I18n.get('ADMIN_LDAP_TITLE'),
    description: NX.I18n.get('ADMIN_LDAP_SUBTITLE'),
    view: { xtype: 'nx-coreui-ldapserver-feature' },
    iconConfig: {
      file: 'book_addresses.png',
      variants: ['x16', 'x32']
    },
    visible: function() {
      return NX.Permissions.check('security:ldapconfig', 'read');
    }
  },
  permission: 'security:ldapconfig',

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.callParent();

    me.listen({
      controller: {
        '#Refresh': {
          refresh: me.loadTemplates
        }
      },
      component: {
        'nx-coreui-ldapserver-list': {
          beforerender: me.loadTemplates
        },
        'nx-coreui-ldapserver-list button[action=new]': {
          click: me.showAddWindow
        },
        'nx-coreui-ldapserver-add form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-ldapserver-connection-form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-ldapserver-backup-form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-ldapserver-userandgroup-form': {
          submitted: me.onSettingsSubmitted
        },
        'nx-coreui-ldapserver-list button[action=changeorder]': {
          click: me.showChangeOrder,
          afterrender: me.bindChangeOrderButton
        },
        'nx-coreui-ldapserver-list button[action=clearcache]': {
          click: me.clearCache,
          afterrender: me.bindClearCacheButton
        },
        'nx-coreui-ldapserver-changeorder button[action=save]': {
          click: me.changeOrder
        },
        'nx-coreui-ldapserver-add button[action=verifyconnection]': {
          click: me.verifyConnection
        },
        'nx-coreui-ldapserver-connection button[action=verifyconnection]': {
          click: me.verifyConnection
        },
        'nx-coreui-ldapserver-add button[action=verifyusermapping]': {
          click: me.verifyUserMapping
        },
        'nx-coreui-ldapserver-userandgroup button[action=verifyusermapping]': {
          click: me.verifyUserMapping
        },
        'nx-coreui-ldapserver-add button[action=verifylogin]': {
          click: me.showLoginCredentialsWindow
        },
        'nx-coreui-ldapserver-userandgroup button[action=verifylogin]': {
          click: me.showLoginCredentialsWindow
        },
        'nx-coreui-ldapserver-userandgroup-login-credentials button[action=verifylogin]': {
          click: me.verifyLogin
        }
      }
    });
  },

  /**
   * @override
   */
  getDescription: function(model) {
    return model.get('name');
  },

  /**
   * @override
   */
  onSelection: function(list, model) {
    var me = this;

    if (Ext.isDefined(model)) {
      me.getConnection().loadRecord(model);
      me.getBackup().loadRecord(model);
      me.getUserAndGroup().loadRecord(model);
    }
  },

  /**
   * @private
   */
  showAddWindow: function() {
    var me = this,
      feature = me.getFeature();

    // Show the first panel in the create wizard, and set the breadcrumb
    feature.setItemName(1, NX.I18n.get('ADMIN_LDAP_CREATE_TITLE'));
    me.loadCreateWizard(1, true, Ext.widget({
      xtype: 'panel',
      layout: {
        type: 'vbox',
        align: 'stretch',
        pack: 'start'
      },
      items: [
        { xtype: 'nx-drilldown-actions' },
        {
          xtype: 'nx-coreui-ldapserver-add',
          flex: 1
        }
      ]
    }));
  },

  /**
   * @private
   */
  showChangeOrder: function() {
    Ext.widget('nx-coreui-ldapserver-changeorder');
  },

  /**
   * @private
   * Reload store after add/update.
   */
  onSettingsSubmitted: function(form, action) {
    var me = this,
        win = form.up('nx-coreui-ldapserver-add');

    if (win) {
      me.loadStoreAndSelect(action.result.data.id, false);
    } else {
      me.loadStore(Ext.emptyFn);
    }
  },

  /**
   * @private
   * Load LDAP schema templates store.
   */
  loadTemplates: function() {
    var me = this,
        list = me.getList();

    if (list) {
      me.getLdapSchemaTemplateStore().load();
    }
  },

  /**
   * @private
   * Enable 'Change Order' when user has 'update' permission.
   */
  bindChangeOrderButton: function(button) {
    var me = this;

    button.mon(
        NX.Conditions.isPermitted(me.permission, 'update'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   * Enable 'ClearCache' when user has 'delete' permission and there is at least one LDAP server configured.
   */
  bindClearCacheButton: function(button) {
    var me = this;
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(me.permission, 'delete'),
            NX.Conditions.storeHasRecords('LdapServer')
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @private
   * @override
   * Deletes a LDAP server.
   * @param {NX.coreui.model.LdapServer} model to be deleted
   */
  deleteModel: function(model) {
    var me = this,
        description = me.getDescription(model);

    NX.direct.ldap_LdapServer.remove(model.getId(), function(response) {
      me.loadStore();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: NX.I18n.format('ADMIN_LDAP_DELETE_SUCCESS', description), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Change LDAP servers order.
   */
  changeOrder: function(button) {
    var me = this,
        win = button.up('window'),
        order = button.up('form').down('nx-itemorderer').getValue();

    NX.direct.ldap_LdapServer.changeOrder(order, function(response) {
      if (Ext.isObject(response) && response.success) {
        me.loadStore();
        win.close();
        NX.Messages.add({ text: NX.I18n.get('ADMIN_LDAP_ORDER_SUCCESS'), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Clear LDAP cache.
   */
  clearCache: function(button) {
    NX.direct.ldap_LdapServer.clearCache(function(response) {
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: NX.I18n.get('ADMIN_LDAP_DETAILS_CLEAR_CACHE_SUCCESS'), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Verify LDAP server connection.
   */
  verifyConnection: function(button) {
    var form = button.up('form'),
        values = form.getForm().getFieldValues(),
        url = values.protocol + '://' + values.host + ':' + values.port;

    form.getEl().mask(NX.I18n.format('ADMIN_LDAP_DETAILS_VERIFY_CONNECTION_MASK', url));

    NX.direct.ldap_LdapServer.verifyConnection(values, function(response) {
      form.getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: NX.I18n.format('ADMIN_LDAP_DETAILS_VERIFY_CONNECTION_SUCCESS', url), type: 'success' });
      }
    });
  },

  /**
   * @private
   * Verify LDAP user mapping.
   */
  verifyUserMapping: function(button) {
    var form = button.up('form'),
        values = form.getForm().getFieldValues(),
        url = values.protocol + '://' + values.host + ':' + values.port;

    form.getEl().mask(NX.I18n.format('ADMIN_LDAP_DETAILS_VERIFY_MAPPING_MASK', url));

    NX.direct.ldap_LdapServer.verifyUserMapping(values, function(response) {
      form.getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        NX.Messages.add({ text: NX.I18n.format('ADMIN_LDAP_DETAILS_VERIFY_MAPPING_SUCCESS', url), type: 'success' });
        Ext.widget('nx-coreui-ldapserver-userandgroup-testresults', { mappedUsers: response.data });
      }
    });
  },

  /**
   * @private
   */
  showLoginCredentialsWindow: function(button) {
    var form = button.up('form'),
        values = form.getForm().getFieldValues();

    Ext.widget('nx-coreui-ldapserver-userandgroup-login-credentials', { values: values });
  },

  /**
   * @private
   * Verify LDAP login.
   */
  verifyLogin: function(button) {
    var win = button.up('window'),
        form = button.up('form'),
        loginValues = form.getForm().getFieldValues(),
        userName = NX.util.Base64.encode(loginValues.username),
        userPass = NX.util.Base64.encode(loginValues.password),
        values = win.values,
        url = values.protocol + '://' + values.host + ':' + values.port;

    form.getEl().mask(NX.I18n.format('ADMIN_LDAP_DETAILS_VERIFY_LOGIN_MASK', url));

    NX.direct.ldap_LdapServer.verifyLogin(values, userName, userPass, function(response) {
      form.getEl().unmask();
      if (Ext.isObject(response) && response.success) {
        win.close();
        NX.Messages.add({ text: NX.I18n.format('ADMIN_LDAP_DETAILS_VERIFY_LOGIN_SUCCESS', url), type: 'success' });
      }
    });
  }

});
