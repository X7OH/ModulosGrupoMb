OB.UTIL.HookManager.registerHook("OBPOS_LineSelected", function (args, c) {
  var line = args.line;
  var context = args.context;
  var affected = ComboModificationUtils.isAffectedByComo(line);

  if (affected) {
    context.enableKeyboardButton(false);
  }

  OB.UTIL.HookManager.callbackExecutor(args, c);
});

OB.UTIL.HookManager.registerHook('OBPOS_PostAddProductToOrder', function (args, c) {
  var orderline = args.orderline;

  if (args.newLine) {
    var splitline = args.options && args.options.splitline === true;
    var comboId = args.options && args.options.comboId;
    if (splitline && comboId) {
      orderline.set('splitline', true);
      //orderline.set('isEditable', false);
      orderline.set('comboId', comboId);
      orderline.set('isExtraSupplement', args.options.isExtraSupplement)
    }
  }

  OB.UTIL.HookManager.callbackExecutor(args, c);
});

//TODO de momento no hace nada pero podría usarse para darles color a las lineas con Combos
OB.UTIL.HookManager.registerHook('OBPOS_RenderOrderLine', function (args, c) {
  var orderline = args.orderline;

  if (orderline.model.get('promotions')) {
    if (ComboModificationUtils.isAffectedByComo(orderline.model)) {
      //TODO de momento no lo cambio 
      // orderline.style = "background-color: rgba(108, 179, 63, 0.5);"

      enyo.forEach(orderline.model.get('promotions'), function (d) {
        var identifierName = d.identifier || d.name;
        var nochunks = d.chunks;
        if (d.hidden) {
          orderline.createComponent({
            style: 'display: block; padding-top: 4px;',
            components: [{
              content: (OB.UTIL.isNullOrUndefined(nochunks) || nochunks === 1) ? '-- ' + identifierName : '-- ' + '(' + nochunks + 'x) ' + identifierName,
              attributes: {
                style: 'float: left; width: 80%; clear: left;'
              }
            }, {
              content: OB.I18N.formatCurrency(-d.amt),
              attributes: {
                style: 'float: right; width: 20%; text-align: right;'
              }
            }, {
              style: 'clear: both;'
            }]
          });
        }
      }, orderline);
    }
  }
  OB.UTIL.HookManager.callbackExecutor(args, c);
});