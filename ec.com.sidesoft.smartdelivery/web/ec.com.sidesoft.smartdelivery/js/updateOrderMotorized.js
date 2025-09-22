OB.OBPFCI = {};
OB.OBPFCI.ClientSideEventHandlers = {};
OB.OBPFCI.SSMRDR_MOTORIZED_HEADER_TAB = '3C0B9D6241EA45178B0E3711D571D78D';
 
OB.OBPFCI.ClientSideEventHandlers.updateOrderMotorized = function (view, form, grid, extraParameters, actions) {
  var data = extraParameters.data, orderId, callback;
  var viewInGridMode = !view.isShowingForm;
 
  if (extraParameters.isNewRecord) {
    // Save flow
    orderId = data.id;
  } else {
    // Update flow
    orderId = data.id;

    callback = function (response, cdata, request) {
      OB.EventHandlerRegistry.callbackExecutor(view, form, grid, extraParameters, actions);
    };
   
    // Calling action handler
    OB.RemoteCallManager.call('ec.com.sidesoft.smartdelivery.ad_actions.UpdateOrderMotorizedActionHandler', {
      orderId: orderId
    }, {}, callback); 
    
    if (viewInGridMode) {
      grid.refreshGridFromClientEventHandler(callback);
    }    
  }
};
 
OB.EventHandlerRegistry.register(OB.OBPFCI.SSMRDR_MOTORIZED_HEADER_TAB, OB.EventHandlerRegistry.POSTSAVE, OB.OBPFCI.ClientSideEventHandlers.updateOrderMotorized, 'SSMRDR_UpdateOrderMotorized');