package com.kevinsimard.aws.lambda;

import com.amazonaws.services.lambda.runtime.*;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public final class Runner<I, O> {

    public static <I, O> void main(final String[] args) throws Exception {
        Context context = new Context() {
            public String getAwsRequestId() {
                return null;
            }

            public String getLogGroupName() {
                return null;
            }

            public String getLogStreamName() {
                return null;
            }

            public String getFunctionName() {
                return null;
            }

            public String getFunctionVersion() {
                return null;
            }

            public String getInvokedFunctionArn() {
                return null;
            }

            public CognitoIdentity getIdentity() {
                return null;
            }

            public ClientContext getClientContext() {
                return null;
            }

            public int getRemainingTimeInMillis() {
                return 0;
            }

            public int getMemoryLimitInMB() {
                return 0;
            }

            public LambdaLogger getLogger() {
                return null;
            }
        };

        if (args.length != 2) {
            throw new RuntimeException("Invalid number of arguments");
        }

        Object object;
        String handlerClass = args[0];

        try {
            Class<?> clazz = Class.forName(handlerClass);
            Constructor<?> constructor = clazz.getConstructor();
            object = constructor.newInstance();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format("%s not found in classpath", handlerClass));
        }

        if (! (object instanceof RequestHandler)) {
            throw new RuntimeException(String.format(
                "Request handler class does not implement %s interface",
                RequestHandler.class.toString()
            ));
        }

        @SuppressWarnings("unchecked")
        RequestHandler<I, O> requestHandler = (RequestHandler<I, O>) object;

        I requestObject = getRequestObject(requestHandler, args[1]);

        try {
            requestHandler.handleRequest(requestObject, context);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static <I> I getRequestObject(RequestHandler handler, String json) throws IOException {
        Type requestType = null;

        for (Type genericInterface : handler.getClass().getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType) {
                Type[] genericTypes = ((ParameterizedType) genericInterface).getActualTypeArguments();
                requestType = genericTypes[0];
            }
        }

        if (null == requestType) return null;

        ObjectMapper mapper = new ObjectMapper();
        JavaType type = mapper.getTypeFactory().constructType(requestType);

        try {
            return mapper.readValue(json, type);
        } catch (RuntimeException e) {
            return mapper.readValue("{}", type);
        }
    }
}
