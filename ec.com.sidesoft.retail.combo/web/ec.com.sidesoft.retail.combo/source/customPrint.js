OB.DS.HWServer.prototype.print = function (template, params, callback, device) {
  if (template) {
    if (template.getData) {
      var me = this;
      template.getData(function (data) {
        if (params.order) {
          var order = params.order;
          debugger;
          var lines = order.get('lines'),
            line, promotions, auxLines;
          var finalLines = [];

          for (var i = 0; i < lines.length; i++) {
            line = lines.at(i);
            if (line.get('isExtraSupplement')) {
              continue;
            }
            promotions = line.get('promotions');
            var comboPromotion = null;
            if (promotions) {
              for (var j = 0; j < promotions.length; j++) {
                if (promotions[j].discountType === OB.OBCOMBO.comboRuleId || promotions[j].discountType === OB.OBCOMBO.comboFixPrice.comboRuleId) {
                  comboPromotion = promotions[j];
                  break;
                }
              }
            }

            if (comboPromotion) {

              var alreadyAdded = false;
              var alreadyAddedLine = undefined;
              for (var j = 0; j < finalLines.length; j++) {
                if (finalLines[j].id = line.get('comboId')) {
                  alreadyAdded = true;
                  alreadyAddedLine = finalLines[j];
                }
              }
              if (alreadyAdded) {
                if (order.get('priceIncludesTax')) {
                  alreadyAddedLine.set('gross', line.get('gross') - comboPromotion.amt);
                } else {
                  alreadyAddedLine.set('net', line.get('net') - promotionInternal.amt);
                }
              } else {
                var comboName = comboPromotion.name;
                var product = line.get('product');
                product.set("_identifier", comboName);
                if (order.get('priceIncludesTax')) {
                  line.set('gross', line.get('gross') - comboPromotion.amt);
                } else {
                  line.set('net', line.get('net') - promotionInternal.amt);
                }

                line.set('promotions', []);
                finalLines.push({
                  id: line.get('comboId'),
                  line: line
                });
              }
            } else {
              finalLines.push({
                id: line.get('id'),
                line: line
              });
            }
          }


          for (var i = 0; i < lines.length; i++) {
            line = lines.at(i);
            if (!line.get('isExtraSupplement')) {
              continue;
            }

            promotions = line.get('promotions');
            var comboPromotion = null;
            if (promotions) {
              for (var j = 0; j < promotions.length; j++) {
                if (
                  promotions[j].discountType === OB.OBCOMBO.comboRuleId || promotions[j].discountType === OB.OBCOMBO.comboFixPrice.comboRuleId) {
                  comboPromotion = promotions[j];
                  break;
                }
              }
            }

            if (comboPromotion) {
              //Recorrer el array de lineas finales y adjuntarles las suplementarias
              for (var j = 0; j < finalLines.length; j++) {
                var finalLine = finalLines[j];

                if (line.get('comboId') == finalLine.id) {
                  var internalLine = finalLine.line;

                  if (order.get('priceIncludesTax')) {
                    line.set('gross', line.get('gross') - comboPromotion.amt);
                  } else {
                    line.set('net', line.get('net') - promotionInternal.amt);
                  }

                  line.set('promotions', []);
                  if (internalLine.get('supplementLines')) {
                    var supplementLines = internalLine.get('supplementLines');
                    supplementLines.push(line);
                  } else {
                    internalLine.set('supplementLines', [line]);
                  }

                }
              }
            }
          }

          auxLines = finalLines.map(function (item) {
            return item.line;
          });
        }
        me.print(data, params, callback, device);
      });
    } else {
      this._print(template, params, callback, device);
    }
  }
};