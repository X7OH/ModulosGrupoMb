/**
 * WS_ImpresionMultipleLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ec.com.sidesoft.retail.multiple.print.ad_process.WebService;

public class WS_ImpresionMultipleLocator extends org.apache.axis.client.Service implements ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_ImpresionMultiple {

    public WS_ImpresionMultipleLocator() {
    }


    public WS_ImpresionMultipleLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public WS_ImpresionMultipleLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for WS_ImpresionPort
    private java.lang.String WS_ImpresionPort_address = null;
    
    public void setUrl(String Url) {
        System.out.println(Url);
        WS_ImpresionPort_address = Url;
    }

    public java.lang.String getWS_ImpresionPortAddress() {
        return WS_ImpresionPort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String WS_ImpresionPortWSDDServiceName = "WS_ImpresionPort";

    public java.lang.String getWS_ImpresionPortWSDDServiceName() {
        return WS_ImpresionPortWSDDServiceName;
    }

    public void setWS_ImpresionPortWSDDServiceName(java.lang.String name) {
        WS_ImpresionPortWSDDServiceName = name;
    }

    public ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_Impresion getWS_ImpresionPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(WS_ImpresionPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getWS_ImpresionPort(endpoint);
    }

    public ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_Impresion getWS_ImpresionPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_ImpresionPortBindingStub _stub = new ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_ImpresionPortBindingStub(portAddress, this);
            _stub.setPortName(getWS_ImpresionPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setWS_ImpresionPortEndpointAddress(java.lang.String address) {
        WS_ImpresionPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_Impresion.class.isAssignableFrom(serviceEndpointInterface)) {
                ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_ImpresionPortBindingStub _stub = new ec.com.sidesoft.retail.multiple.print.ad_process.WebService.WS_ImpresionPortBindingStub(new java.net.URL(WS_ImpresionPort_address), this);
                _stub.setPortName(getWS_ImpresionPortWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("WS_ImpresionPort".equals(inputPortName)) {
            return getWS_ImpresionPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://impresion.sidesoft.com.ec/", "WS_ImpresionMultiple");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://impresion.sidesoft.com.ec/", "WS_ImpresionPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("WS_ImpresionPort".equals(portName)) {
            setWS_ImpresionPortEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
