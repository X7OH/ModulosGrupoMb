/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

OB.Utilities.Action.set('showCopyStoreLog', function (paramObj) {
  var i, processView = paramObj._processView,
      mainLayout = processView.resultLayout,
      log = paramObj.log,
      msg = paramObj.msg,
      buttons, logForm, logPopup;

  // setting the message here to be able to set it also in popup
  processView.messageBar.setMessage(msg.msgType, msg.msgTitle, msg.msgText);

  if (paramObj.disableFields) {
    // we are in popup, we want it to be kept open but disabling its fields
    processView.disableFormItems();
    buttons = processView.popupButtons.getMembers()[0].getMembers();
    buttons[1].hide(); // hide Done button
  }

  logForm = isc.OBPOSCS_LogForm.create({
    log: log,
    rowSpan: 10,
    colSpan: 4,
    width: mainLayout ? '*' : '100%'
  });

  if (mainLayout) {
    // we are after PR14Q2: let's show the results within the params page
    mainLayout.addMember(logForm);
  } else {
    // previous to PR14Q2: display results in popup 
    logPopup = isc.OBPopup.create({
      isModal: false,
      showMinimizeButton: false,
      showMaximizeButton: false
    });
    logPopup.addItem(logForm);
    logPopup.show();
  }
});

isc.defineClass('OBPOSCS_LogForm', isc.DynamicForm).addProperties({
  width: '*',
  numCols: 4,
  colWidths: ['25%', '25%', '25%', '25%'],

  initWidget: function () {
    var rowSpan = this.rowSpan,
        colSpan = this.colSpan,
        undef;
    this.rowSpan = undef;
    this.colSpan = undef;
    this.Super('initWidget', arguments);
    this.addFields([{
      defaultValue: OB.I18N.getLabel('OBPOSCS_LogTabTitle'),
      type: 'OBSectionItem',
      sectionExpanded: true,
      itemIds: ['_logFormItem']
    }, {
      type: 'OBPOSCS_LogFormItem',
      log: this.log,
      rowSpan: rowSpan,
      colSpan: colSpan
    }]);
  }
});

isc.defineClass('OBPOSCS_LogFormItem', isc.CanvasItem).addProperties({
  name: '_logFormItem',
  showTitle: false,
  init: function () {
    var undef, i;

    this._rowSpan = this.rowSpan || 1;
    this.rowSpan = undef;

    this.canvas = isc.VLayout.create({
      overflow: 'auto',
      showEdges: true,
      edgeSize: 1,
      errorOrientation: 'left',

      styleName: 'OBFormFieldImageInput'
    });

    for (i = 0; i < this.log.length; i++) {
      this.addLogEntry(this.log[i]);
    }

    this.colSpan = this.colSpan || 3;
    this.disabled = false;

    this.Super('init', arguments);
    this.initStyle();
  },

  initStyle: function () {
    this.height = 30 * this._rowSpan;
  },

  addLogEntry: function (logEntry) {
    var logTxt, icon, iconImg;
    logTxt = '<span>' + logEntry.msg;
    if (logEntry.link) {
      logTxt += ' <a href="#" onclick="OB.Utilities.openDirectView(null, \'' + logEntry.link.keyColumn + '\', \'' + logEntry.link.targetEntity + '\',\'' + logEntry.link.recordId + '\')">' + OB.I18N.getLabel('OBPOSCS_OpenRecord') + '</a>';
    }

    icon = '<div class="OBPOSCSLog"><img class="OBPOSCSLog' + logEntry.level + '" src="' + OB.Styles.skinsPath + 'Default/org.openbravo.client.application/images/blank/blank.gif">';

    logTxt = icon + logTxt + '</div>';

    this.canvas.addMember(isc.Label.create({
      height: 15,
      wrap: true,
      contents: logTxt
    }));
  }
});