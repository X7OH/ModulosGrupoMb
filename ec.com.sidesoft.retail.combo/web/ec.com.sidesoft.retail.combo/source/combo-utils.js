var ComboModificationUtils = {};

ComboModificationUtils.isAffectedByComo = function (line) {
  if (line) {
    var promotions = line.get("promotions");
    if (promotions) {
      for (var i = 0; i < promotions.length; i++) {
        if (
          promotions[i].discountType === OB.OBCOMBO.comboRuleId || promotions[i].discountType === OB.OBCOMBO.comboFixPrice.comboRuleId) {
          return promotions[i];
        }
      }
    }
  }
  return false;
};

ComboModificationUtils.getLinesWithCombo = function (combo, order) {
  var lines = order.get('lines').filter(function (lineSelected) {
    if (lineSelected && lineSelected.get('promotions')) {
      var promotions = lineSelected.get('promotions');
      for (var i = 0; i < promotions.length; i++) {
        if (promotions[i].ruleId === combo) {
          return true;
        }
      }
      return false;
    }
  });

  return lines;
}


