package edu.ucsb.eucalyptus.admin.client.ImageStore;

import com.google.gwt.user.client.rpc.AsyncCallback;


public interface ImageStoreServiceAsync {
    void requestJSON(String sessionId, ImageStoreService.Method method,
                     String uri, ImageStoreService.Parameter[] params,
                     AsyncCallback<String> callback);
}
