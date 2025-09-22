OB.Model.Order.prototype.groupLinesByProduct = function () {
  var lineToMerge, lines = this.get('lines');

  var auxLines = lines.models.slice(0),
    localSkipApplyPromotions = this.get('skipApplyPromotions');

  auxLines = auxLines.filter(function (line) {
    return !line.get('comboId');
  })
  this.set({
    'skipApplyPromotions': true
  }, {
    silent: true
  });
  _.each(auxLines, function (l) {
    lineToMerge = _.find(lines.models, function (line) {
      if (l !== line && l.get('product').id === line.get('product').id && l.get('price') === line.get('price') && line.get('qty') > 0 && l.get('qty') > 0 && !_.find(line.get('promotions'), function (promo) {
          return promo.manual;
        }) && !_.find(l.get('promotions'), function (promo) {
          return promo.manual;
        })) {
        if (!line.get('comboId')) {
          return line;
        }
      }
    });
    //When it Comes To Technically , Consider The Product As Non-Grouped When scaled and groupproduct Are Checked 
    if (lineToMerge && lineToMerge.get('product').get('groupProduct') && !(lineToMerge.get('product').get('groupProduct') && lineToMerge.get('product').get('obposScale'))) {
      lineToMerge.set({
        qty: lineToMerge.get('qty') + l.get('qty')
      }, {
        silent: true
      });
      lines.remove(l);
    }
  });
  this.set({
    'skipApplyPromotions': localSkipApplyPromotions
  }, {
    silent: true
  });
};


OB.Model.OrderLine.prototype.isAffectedByPack = function () {

  return _.find(this.get('promotions'), function (promotion) {

    if (promotion.pack && promotion.discountType !== OB.OBCOMBO.comboRuleId && promotion.discountType !== OB.OBCOMBO.comboFixPrice.comboRuleId) {
      return true;
    }
  }, this);
}