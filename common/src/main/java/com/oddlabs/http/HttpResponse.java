package com.oddlabs.http;

import java.io.Serializable;

interface HttpResponse extends Serializable {
    void notify(HttpCallback callback);
}
