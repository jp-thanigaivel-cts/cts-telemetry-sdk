package com.example.demo2.service;

import com.example.demo.proto.HelloRequest;
import com.example.demo.proto.HelloReply;
import com.example.demo.proto.HelloServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class HelloGrpcService extends HelloServiceGrpc.HelloServiceImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        HelloReply reply = HelloReply.newBuilder()
                .setMessage("Hello " + request.getName() + " from App2 gRPC!")
                .build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}
