package ec.com.sidesoft.retail.multiple.print.ad_process.WebService;

public class WS_ImpresionProxy implements ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_Impresion {
  private String _endpoint = null;
  private ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_Impresion wS_Impresion = null;
  
  public WS_ImpresionProxy() {
    _initWS_ImpresionProxy();
  }
  
  public WS_ImpresionProxy(String endpoint) {
    _endpoint = endpoint;
    _initWS_ImpresionProxy();
  }
  
  private void _initWS_ImpresionProxy() {
    try {
      wS_Impresion = (new ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_ImpresionMultipleLocator()).getWS_ImpresionPort();
      if (wS_Impresion != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)wS_Impresion)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)wS_Impresion)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (wS_Impresion != null)
      ((javax.xml.rpc.Stub)wS_Impresion)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_Impresion getWS_Impresion() {
    if (wS_Impresion == null)
      _initWS_ImpresionProxy();
    return wS_Impresion;
  }
  
  public boolean impresion_multiple(java.lang.String[] lstFactura, java.lang.String[] lstComanda, java.lang.String strDocumentno) throws java.rmi.RemoteException{
    if (wS_Impresion == null)
      _initWS_ImpresionProxy();
    return wS_Impresion.impresion_multiple(lstFactura, lstComanda, strDocumentno);
  }
  
  
}