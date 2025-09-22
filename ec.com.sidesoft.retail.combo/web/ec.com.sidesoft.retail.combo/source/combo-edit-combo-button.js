enyo.kind({
  kind: "OB.UI.SmallButton",
  name: "OB.OBPOSPointOfSale.UI.EditLine.EditCombo",
  content: "",
  classes: "combo-edit-button",
  tap: function () {
    var comboPromotions = this.owner.owner.line.get("promotions").filter(
      function (promotion) {
        return (promotion.discountType === OB.OBCOMBO.comboRuleId ||
          promotion.discountType === OB.OBCOMBO.comboFixPrice.comboRuleId)
      }
    );

    var popupInstance = OB.POS.terminal.$.containerWindow.getRoot().$.OBCOMBO_Popup;

    OB.POS.terminal.$.containerWindow.getRoot().doShowPopup({
      popup: "OBCOMBO_Popup",
      args: {
        comboId: comboPromotions[0].ruleId,
        receipt: this.owner.owner.receipt
      }
    });
    popupInstance.loadFamiliesWithValues(
      comboPromotions[0].name, comboPromotions[0].ruleId, this.owner.owner.receipt);
  },
  init: function (model) {
    this.model = model;
    this.model.get("order").get("lines").on("selected", function (lineSelected) {
      if (lineSelected && lineSelected.get("promotions")) {
        var promotions = lineSelected.get("promotions");
        var count = 0;
        for (var i = 0; i < promotions.length; i++) {
          if (
            promotions[i].discountType === OB.OBCOMBO.comboRuleId || promotions[i].discountType === OB.OBCOMBO.comboFixPrice.comboRuleId) {
            count++;
          }
        }
        if (count == 1) {
          this.setShowing(true);
          return;
        }
        this.setShowing(false);
        return;
      }
      this.setShowing(false);
    }, this);
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel("SSRCM_edit_combo"));

    this.show();
  }
});

OB.OBPOSPointOfSale.UI.EditLine.prototype.actionButtons.unshift({
  kind: "OB.OBPOSPointOfSale.UI.EditLine.EditCombo",
  name: "EditComboButton"
});