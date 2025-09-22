enyo.kind({
  kind: "OB.UI.SmallButton",
  name: "OB.OBPOSPointOfSale.UI.EditLine.DeleteCombo",
  content: "",
  classes: "combo-delete-button",
  tap: function () {
    var comboPromotions = this.owner.owner.line.get("promotions").filter(
      function (promotion) {
        return (promotion.discountType === OB.OBCOMBO.comboRuleId ||
          promotion.discountType === OB.OBCOMBO.comboFixPrice.comboRuleId)
      }
    );
    var lines = this.owner.owner.receipt.get('lines').filter(function (lineSelected) {
      if (lineSelected && lineSelected.get('promotions')) {
        var promotions = lineSelected.get('promotions');
        for (var i = 0; i < promotions.length; i++) {
          if (promotions[i].ruleId === comboPromotions[0].ruleId) {
            return true;
          }
        }
        return false;
      }
    });

    this.owner.owner.receipt.deleteLinesFromOrder(lines);
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
    this.setContent(OB.I18N.getLabel("OBPOS_ButtonDelete"));

    this.show();
  }
});

OB.OBPOSPointOfSale.UI.EditLine.prototype.actionButtons.unshift({
  kind: "OB.OBPOSPointOfSale.UI.EditLine.DeleteCombo",
  name: "DeleteComboButton"
});