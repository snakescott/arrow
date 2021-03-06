/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

<@pp.dropOutputFile />
<#list ["Nullable", "Single"] as mode>
<@pp.changeOutputFile name="/org/apache/arrow/vector/complex/impl/${mode}MapWriter.java" />
<#assign index = "idx()">
<#if mode == "Single">
<#assign containerClass = "MapVector" />
<#else>
<#assign containerClass = "NullableMapVector" />
</#if>

<#include "/@includes/license.ftl" />

package org.apache.arrow.vector.complex.impl;

<#include "/@includes/vv_imports.ftl" />
import java.util.Map;

import org.apache.arrow.vector.holders.RepeatedMapHolder;
import org.apache.arrow.vector.AllocationHelper;
import org.apache.arrow.vector.complex.reader.FieldReader;
import org.apache.arrow.vector.complex.writer.FieldWriter;

import com.google.common.collect.Maps;

/*
 * This class is generated using FreeMarker and the ${.template_name} template.
 */
@SuppressWarnings("unused")
public class ${mode}MapWriter extends AbstractFieldWriter {

  protected final ${containerClass} container;
  private final Map<String, FieldWriter> fields = Maps.newHashMap();
  public ${mode}MapWriter(${containerClass} container) {
    <#if mode == "Single">
    if (container instanceof NullableMapVector) {
      throw new IllegalArgumentException("Invalid container: " + container);
    }
    </#if>
    this.container = container;
    for (Field child : container.getField().getChildren()) {
      MinorType minorType = Types.getMinorTypeForArrowType(child.getType());
      switch (minorType) {
      case MAP:
        map(child.getName());
        break;
      case LIST:
        list(child.getName());
        break;
      case UNION:
        UnionWriter writer = new UnionWriter(container.addOrGet(child.getName(), FieldType.nullable(MinorType.UNION.getType()), UnionVector.class), getNullableMapWriterFactory());
        fields.put(handleCase(child.getName()), writer);
        break;
<#list vv.types as type><#list type.minor as minor>
<#assign lowerName = minor.class?uncap_first />
<#if lowerName == "int" ><#assign lowerName = "integer" /></#if>
<#assign upperName = minor.class?upper_case />
      case ${upperName}: {
        <#if minor.typeParams?? >
        ${minor.arrowType} arrowType = (${minor.arrowType})child.getType();
        ${lowerName}(child.getName()<#list minor.typeParams as typeParam>, arrowType.get${typeParam.name?cap_first}()</#list>);
        <#else>
        ${lowerName}(child.getName());
        </#if>
        break;
      }
</#list></#list>
        default:
          throw new UnsupportedOperationException("Unknown type: " + minorType);
      }
    }
  }

  protected String handleCase(final String input) {
    return input.toLowerCase();
  }

  protected NullableMapWriterFactory getNullableMapWriterFactory() {
    return NullableMapWriterFactory.getNullableMapWriterFactoryInstance();
  }

  @Override
  public int getValueCapacity() {
    return container.getValueCapacity();
  }

  @Override
  public boolean isEmptyMap() {
    return 0 == container.size();
  }

  @Override
  public Field getField() {
      return container.getField();
  }

  @Override
  public MapWriter map(String name) {
    String finalName = handleCase(name);
    FieldWriter writer = fields.get(finalName);
    if(writer == null){
      int vectorCount=container.size();
      NullableMapVector vector = container.addOrGet(name, FieldType.nullable(MinorType.MAP.getType()), NullableMapVector.class);
      writer = new PromotableWriter(vector, container, getNullableMapWriterFactory());
      if(vectorCount != container.size()) {
        writer.allocate();
      }
      writer.setPosition(idx());
      fields.put(finalName, writer);
    } else {
      if (writer instanceof PromotableWriter) {
        // ensure writers are initialized
        ((PromotableWriter)writer).getWriter(MinorType.MAP);
      }
    }
    return writer;
  }

  @Override
  public void close() throws Exception {
    clear();
    container.close();
  }

  @Override
  public void allocate() {
    container.allocateNew();
    for(final FieldWriter w : fields.values()) {
      w.allocate();
    }
  }

  @Override
  public void clear() {
    container.clear();
    for(final FieldWriter w : fields.values()) {
      w.clear();
    }
  }

  @Override
  public ListWriter list(String name) {
    String finalName = handleCase(name);
    FieldWriter writer = fields.get(finalName);
    int vectorCount = container.size();
    if(writer == null) {
      writer = new PromotableWriter(container.addOrGet(name, FieldType.nullable(MinorType.LIST.getType()), ListVector.class), container, getNullableMapWriterFactory());
      if (container.size() > vectorCount) {
        writer.allocate();
      }
      writer.setPosition(idx());
      fields.put(finalName, writer);
    } else {
      if (writer instanceof PromotableWriter) {
        // ensure writers are initialized
        ((PromotableWriter)writer).getWriter(MinorType.LIST);
      }
    }
    return writer;
  }

  public void setValueCount(int count) {
    container.getMutator().setValueCount(count);
  }

  @Override
  public void setPosition(int index) {
    super.setPosition(index);
    for(final FieldWriter w: fields.values()) {
      w.setPosition(index);
    }
  }

  @Override
  public void start() {
    <#if mode == "Single">
    <#else>
    container.getMutator().setIndexDefined(idx());
    </#if>
  }

  @Override
  public void end() {
    setPosition(idx()+1);
  }

  <#list vv.types as type><#list type.minor as minor>
  <#assign lowerName = minor.class?uncap_first />
  <#if lowerName == "int" ><#assign lowerName = "integer" /></#if>
  <#assign upperName = minor.class?upper_case />
  <#assign capName = minor.class?cap_first />
  <#assign vectName = capName />
  <#assign vectName = "Nullable${capName}" />

  <#if minor.typeParams?? >
  @Override
  public ${minor.class}Writer ${lowerName}(String name) {
    // returns existing writer
    final FieldWriter writer = fields.get(handleCase(name));
    assert writer != null;
    return writer;
  }

  @Override
  public ${minor.class}Writer ${lowerName}(String name<#list minor.typeParams as typeParam>, ${typeParam.type} ${typeParam.name}</#list>) {
  <#else>
  @Override
  public ${minor.class}Writer ${lowerName}(String name) {
  </#if>
    FieldWriter writer = fields.get(handleCase(name));
    if(writer == null) {
      ValueVector vector;
      ValueVector currentVector = container.getChild(name);
      ${vectName}Vector v = container.addOrGet(name, 
          FieldType.nullable(
          <#if minor.typeParams??>
            <#if minor.arrowTypeConstructorParams??>
              <#assign constructorParams = minor.arrowTypeConstructorParams />
            <#else>
              <#assign constructorParams = [] />
              <#list minor.typeParams as typeParam>
                <#assign constructorParams = constructorParams + [ typeParam.name ] />
              </#list>
            </#if>    
            new ${minor.arrowType}(${constructorParams?join(", ")})
          <#else>
            MinorType.${upperName}.getType()
          </#if>
          ),
          ${vectName}Vector.class);
      writer = new PromotableWriter(v, container, getNullableMapWriterFactory());
      vector = v;
      if (currentVector == null || currentVector != vector) {
        vector.allocateNewSafe();
      } 
      writer.setPosition(idx());
      fields.put(handleCase(name), writer);
    } else {
      if (writer instanceof PromotableWriter) {
        // ensure writers are initialized
        ((PromotableWriter)writer).getWriter(MinorType.${upperName});
      }
    }
    return writer;
  }

  </#list></#list>

}
</#list>
