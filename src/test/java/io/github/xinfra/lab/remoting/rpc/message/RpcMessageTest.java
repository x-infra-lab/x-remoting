package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.rpc.exception.RpcServerException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RpcMessageTest {

    @Test
    public void testRpcRequest1() throws SerializeException, DeserializeException {
        String content = "this is rpc content";
        String contentType = content.getClass().getName();
        RpcMessageHeader header = new RpcMessageHeader();
        header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));

        Integer requestId = IDGenerator.nextRequestId();
        RpcRequestMessage requestMessage = new RpcRequestMessage(requestId);

        requestMessage.setHeader(header);
        requestMessage.setContent(content);
        requestMessage.setContentType(contentType);

        requestMessage.serialize();

        Assertions.assertNotNull(requestMessage.getContentTypeData());
        Assertions.assertEquals(requestMessage.getContentTypeData().length, requestMessage.getContentTypeLength());

        Assertions.assertNotNull(requestMessage.getHeaderData());
        Assertions.assertEquals(requestMessage.getHeaderData().length, requestMessage.getHeaderLength());

        Assertions.assertNotNull(requestMessage.getContentData());
        Assertions.assertEquals(requestMessage.getContentData().length, requestMessage.getContentLength());


        RpcRequestMessage requestMessage2 = new RpcRequestMessage(requestId);
        requestMessage2.setContentTypeData(requestMessage.getContentTypeData());
        requestMessage2.setHeaderData(requestMessage.getHeaderData());
        requestMessage2.setContentData(requestMessage.getContentData());
        requestMessage2.deserialize();

        Assertions.assertEquals(requestMessage2.getContentType(), requestMessage.getContentType());
        Assertions.assertEquals(requestMessage2.getHeader(), requestMessage.getHeader());
        Assertions.assertEquals(requestMessage2.getContent(), requestMessage.getContent());
    }

    @Test
    public void testRpcResponse1() throws SerializeException, DeserializeException {
        String content = "this is rpc content";
        String contentType = content.getClass().getName();
        RpcMessageHeader header = new RpcMessageHeader();
        header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));

        Integer requestId = IDGenerator.nextRequestId();
        RpcResponseMessage responseMessage = new RpcResponseMessage(requestId);

        responseMessage.setHeader(header);
        responseMessage.setContent(content);
        responseMessage.setContentType(contentType);
        responseMessage.setStatus(ResponseStatus.SUCCESS.getCode());

        responseMessage.serialize();

        Assertions.assertNotNull(responseMessage.getContentTypeData());
        Assertions.assertEquals(responseMessage.getContentTypeData().length, responseMessage.getContentTypeLength());

        Assertions.assertNotNull(responseMessage.getHeaderData());
        Assertions.assertEquals(responseMessage.getHeaderData().length, responseMessage.getHeaderLength());

        Assertions.assertNotNull(responseMessage.getContentData());
        Assertions.assertEquals(responseMessage.getContentData().length, responseMessage.getContentLength());


        RpcResponseMessage responseMessage2 = new RpcResponseMessage(requestId);
        responseMessage2.setContentTypeData(responseMessage.getContentTypeData());
        responseMessage2.setHeaderData(responseMessage.getHeaderData());
        responseMessage2.setContentData(responseMessage.getContentData());

        responseMessage2.deserialize(RpcDeserializeLevel.CONTENT_TYPE);
        Assertions.assertNotNull(responseMessage2.getContentType());
        Assertions.assertNull(responseMessage2.getHeader());
        Assertions.assertNull(responseMessage2.getContent());

        responseMessage2.deserialize(RpcDeserializeLevel.HEADER);
        Assertions.assertNotNull(responseMessage2.getContentType());
        Assertions.assertNotNull(responseMessage2.getHeader());
        Assertions.assertNull(responseMessage2.getContent());

        responseMessage2.deserialize(RpcDeserializeLevel.ALL);


        Assertions.assertEquals(responseMessage2.getContentType(), responseMessage.getContentType());
        Assertions.assertEquals(responseMessage2.getHeader(), responseMessage.getHeader());
        Assertions.assertEquals(responseMessage2.getContent(), responseMessage.getContent());
    }

    @Test
    public void testExceptionRpcResponse1() throws SerializeException, DeserializeException {
        int requestId = IDGenerator.nextRequestId();
        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();
        RpcResponseMessage responseMessage = rpcMessageFactory.createExceptionResponse(requestId,
                new RuntimeException("testCreateExceptionResponse1"),
                ResponseStatus.SERVER_DESERIAL_EXCEPTION);


        responseMessage.serialize();

        Assertions.assertNotNull(responseMessage.getContentTypeData());
        Assertions.assertEquals(responseMessage.getContentTypeData().length, responseMessage.getContentTypeLength());

        Assertions.assertNotNull(responseMessage.getContentData());
        Assertions.assertEquals(responseMessage.getContentData().length, responseMessage.getContentLength());



        RpcResponseMessage responseMessage2 = new RpcResponseMessage(requestId);
        responseMessage2.setContentTypeData(responseMessage.getContentTypeData());
        responseMessage2.setContentData(responseMessage.getContentData());

        responseMessage2.deserialize(RpcDeserializeLevel.CONTENT_TYPE);
        Assertions.assertNotNull(responseMessage2.getContentType());
        Assertions.assertNull(responseMessage2.getContent());

        responseMessage2.deserialize(RpcDeserializeLevel.HEADER);
        Assertions.assertNotNull(responseMessage2.getContentType());
        Assertions.assertNull(responseMessage2.getContent());

        responseMessage2.deserialize(RpcDeserializeLevel.ALL);


        Assertions.assertEquals(responseMessage2.getContentType(), responseMessage.getContentType());
        Assertions.assertEquals(responseMessage2.getHeader(), responseMessage.getHeader());
        Assertions.assertTrue(responseMessage2.getContent() instanceof RpcServerException);

    }
}
