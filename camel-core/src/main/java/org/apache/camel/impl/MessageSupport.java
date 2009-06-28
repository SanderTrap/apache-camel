/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.impl;

import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Message;
import org.apache.camel.TypeConverter;
import org.apache.camel.util.UuidGenerator;

/**
 * A base class for implementation inheritence providing the core
 * {@link Message} body handling features but letting the derived class deal
 * with headers.
 *
 * Unless a specific provider wishes to do something particularly clever with
 * headers you probably want to just derive from {@link DefaultMessage}
 *
 * @version $Revision$
 */
public abstract class MessageSupport implements Message {
    private static final UuidGenerator DEFALT_ID_GENERATOR = new UuidGenerator();
    private Exchange exchange;
    private Object body;
    private String messageId;

    public Object getBody() {
        if (body == null) {
            body = createBody();
        }
        return body;
    }

    public <T> T getBody(Class<T> type) {
        return getBody(type, getBody());
    }

    public Object getMandatoryBody() throws InvalidPayloadException {
        Object answer = getBody();
        if (answer == null) {
            throw new InvalidPayloadException(getExchange(), Object.class, this);
        }
        return answer;
    }

    protected <T> T getBody(Class<T> type, Object body) {
        // eager same instance type test to avoid the overhead of invoking the type converter
        // if already same type
        if (type.isInstance(body)) {
            return type.cast(body);
        }

        Exchange e = getExchange();
        if (e != null) {
            TypeConverter converter = e.getContext().getTypeConverter();

            // lets first try converting the body itself first
            // as for some types like InputStream v Reader its more efficient to do the transformation
            // from the body itself as its got efficient implementations of them, before trying the
            // message
            T answer = converter.convertTo(type, getExchange(), body);
            if (answer != null) {
                return answer;
            }

            // fallback to the message itself (e.g. used in camel-http)
            answer = converter.convertTo(type, getExchange(), this);
            if (answer != null) {
                return answer;
            }
        }

        // not possible to convert
        return null;
    }

    public <T> T getMandatoryBody(Class<T> type) throws InvalidPayloadException {
        // eager same instance type test to avoid the overhead of invoking the type converter
        // if already same type
        if (type.isInstance(body)) {
            return type.cast(body);
        }

        Exchange e = getExchange();
        if (e != null) {
            TypeConverter converter = e.getContext().getTypeConverter();
            try {
                return converter.mandatoryConvertTo(type, e, getBody());
            } catch (Exception cause) {
                throw new InvalidPayloadException(e, type, this, cause);
            }
        }
        throw new InvalidPayloadException(e, type, this);
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public <T> void setBody(Object value, Class<T> type) {
        Exchange e = getExchange();
        if (e != null) {
            T v = e.getContext().getTypeConverter().convertTo(type, e, value);
            if (v != null) {
                value = v;
            }
        }
        setBody(value);
    }

    public Message copy() {
        Message answer = newInstance();
        answer.copyFrom(this);
        return answer;
    }

    public void copyFrom(Message that) {
        setMessageId(that.getMessageId());
        setBody(that.getBody());
        getHeaders().putAll(that.getHeaders());
        getAttachments().putAll(that.getAttachments());
    }

    public Exchange getExchange() {
        return exchange;
    }

    public void setExchange(Exchange exchange) {
        this.exchange = exchange;
    }

    /**
     * Returns a new instance
     */
    public abstract Message newInstance();

    /**
     * A factory method to allow a provider to lazily create the message body
     * for inbound messages from other sources
     *
     * @return the value of the message body or null if there is no value
     *         available
     */
    protected Object createBody() {
        return null;
    }

    public String getMessageId() {
        if (messageId == null) {
            messageId = createMessageId();
        }
        return this.messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * Lets allow implementations to auto-create a messageId
     */
    protected String createMessageId() {
        return DEFALT_ID_GENERATOR.generateId();
    }
}
