/*
 * Copyright 2016, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.grpc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import io.grpc.ServerServiceDefinition.ServerMethodDefinition;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.HashSet;

/** Unit tests for {@link ServerServiceDefinition}. */
@RunWith(JUnit4.class)
public class ServerServiceDefinitionTest {
  private String serviceName = "com.example.service";
  private MethodDescriptor<String, Integer> method1 = MethodDescriptor.create(
      MethodDescriptor.MethodType.UNKNOWN,
      MethodDescriptor.generateFullMethodName(serviceName, "method1"),
      StringMarshaller.INSTANCE, IntegerMarshaller.INSTANCE);
  private MethodDescriptor<String, Integer> diffMethod1 = method1.withIdempotent(true);
  private MethodDescriptor<String, Integer> method2 = MethodDescriptor.create(
      MethodDescriptor.MethodType.UNKNOWN,
      MethodDescriptor.generateFullMethodName(serviceName, "method2"),
      StringMarshaller.INSTANCE, IntegerMarshaller.INSTANCE);
  private ServerCallHandler<String, Integer> methodHandler1
      = new NoopServerCallHandler<String, Integer>();
  private ServerCallHandler<String, Integer> methodHandler2
      = new NoopServerCallHandler<String, Integer>();
  private ServerMethodDefinition<String, Integer> methodDef1
        = ServerMethodDefinition.create(method1, methodHandler1);
  private ServerMethodDefinition<String, Integer> methodDef2
        = ServerMethodDefinition.create(method2, methodHandler2);
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void noMethods() {
    ServiceDescriptor sd = new ServiceDescriptor(serviceName);
    ServerServiceDefinition ssd = ServerServiceDefinition.builder(sd)
        .build();
    assertSame(sd, ssd.getServiceDescriptor());
    assertEquals(Collections.<MethodDescriptor<?, ?>>emptyList(),
        ssd.getServiceDescriptor().getMethods());
  }

  @Test
  public void addMethod_twoArg() {
    ServiceDescriptor sd = new ServiceDescriptor(serviceName, method1, method2);
    ServerServiceDefinition ssd = ServerServiceDefinition.builder(sd)
        .addMethod(method1, methodHandler1)
        .addMethod(method2, methodHandler2)
        .build();
    assertSame(sd, ssd.getServiceDescriptor());
    for (ServerMethodDefinition<?, ?> serverMethod : ssd.getMethods()) {
      MethodDescriptor<?, ?> method = serverMethod.getMethodDescriptor();
      if (method1.equals(method)) {
        assertSame(methodHandler1, serverMethod.getServerCallHandler());
      } else if (method2.equals(method)) {
        assertSame(methodHandler2, serverMethod.getServerCallHandler());
      } else {
        fail("Unexpected method descriptor: " + method.getFullMethodName());
      }
    }
  }

  @Test
  public void addMethod_duplicateName() {
    ServiceDescriptor sd = new ServiceDescriptor(serviceName, method1);
    ServerServiceDefinition.Builder ssd = ServerServiceDefinition.builder(sd)
        .addMethod(method1, methodHandler1);
    thrown.expect(IllegalStateException.class);
    ssd.addMethod(diffMethod1, methodHandler2)
        .build();
  }

  @Test
  public void buildMisaligned_extraMethod() {
    ServiceDescriptor sd = new ServiceDescriptor(serviceName);
    ServerServiceDefinition.Builder ssd = ServerServiceDefinition.builder(sd)
        .addMethod(methodDef1);
    thrown.expect(IllegalStateException.class);
    ssd.build();
  }

  @Test
  public void buildMisaligned_diffMethodInstance() {
    ServiceDescriptor sd = new ServiceDescriptor(serviceName, method1);
    ServerServiceDefinition.Builder ssd = ServerServiceDefinition.builder(sd)
        .addMethod(diffMethod1, methodHandler1);
    thrown.expect(IllegalStateException.class);
    ssd.build();
  }

  @Test
  public void buildMisaligned_missingMethod() {
    ServiceDescriptor sd = new ServiceDescriptor(serviceName, method1);
    ServerServiceDefinition.Builder ssd = ServerServiceDefinition.builder(sd);
    thrown.expect(IllegalStateException.class);
    ssd.build();
  }

  @Test
  public void builderWithServiceName() {
    ServerServiceDefinition ssd = ServerServiceDefinition.builder(serviceName)
        .addMethod(methodDef1)
        .addMethod(methodDef2)
        .build();
    assertEquals(serviceName, ssd.getServiceDescriptor().getName());

    HashSet<MethodDescriptor<?, ?>> goldenMethods = new HashSet<MethodDescriptor<?, ?>>();
    goldenMethods.add(method1);
    goldenMethods.add(method2);
    assertEquals(goldenMethods,
        new HashSet<MethodDescriptor<?, ?>>(ssd.getServiceDescriptor().getMethods()));

    HashSet<ServerMethodDefinition<?, ?>> goldenMethodDefs
        = new HashSet<ServerMethodDefinition<?, ?>>();
    goldenMethodDefs.add(methodDef1);
    goldenMethodDefs.add(methodDef2);
    assertEquals(goldenMethodDefs, new HashSet<ServerMethodDefinition<?, ?>>(ssd.getMethods()));
  }

  @Test
  public void builderWithServiceName_noMethods() {
    ServerServiceDefinition ssd = ServerServiceDefinition.builder(serviceName)
        .build();
    assertEquals(Collections.<MethodDescriptor<?, ?>>emptyList(),
        ssd.getServiceDescriptor().getMethods());
    assertEquals(Collections.<ServerMethodDefinition<?, ?>>emptySet(), ssd.getMethods());
  }

  private static class NoopServerCallHandler<ReqT, RespT>
      implements ServerCallHandler<ReqT, RespT> {
    @Override
    public ServerCall.Listener<ReqT> startCall(ServerCall<ReqT, RespT> call, Metadata headers) {
      throw new UnsupportedOperationException();
    }
  }
}