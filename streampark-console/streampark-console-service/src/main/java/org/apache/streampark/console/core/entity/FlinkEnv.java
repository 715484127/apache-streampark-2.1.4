/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.console.core.entity;

import org.apache.streampark.common.conf.FlinkVersion;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.console.base.exception.ApiAlertException;
import org.apache.streampark.console.base.exception.ApiDetailException;

import org.apache.commons.io.FileUtils;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

@Getter
@Setter
@TableName("t_flink_env")
public class FlinkEnv implements Serializable {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String flinkName;

  private String flinkHome;

  private String flinkConf;

  private String description;

  private String scalaVersion;

  private String version;

  /** is default */
  private Boolean isDefault;

  private Date createTime;

  private transient FlinkVersion flinkVersion;

  private transient String streamParkScalaVersion = scala.util.Properties.versionNumberString();

  public void doSetFlinkConf() throws ApiDetailException {
    File yaml;
    float ver = Float.parseFloat(getVersionOfFirst().concat(".").concat(getVersionOfMiddle()));
    if (ver < 1.19f) {
      yaml = new File(this.flinkHome.concat("/conf/flink-conf.yaml"));
      if (!yaml.exists()) {
        throw new ApiAlertException("cannot find flink-conf.yaml in flink/conf ");
      }
    } else if (ver == 1.19f) {
      yaml = new File(this.flinkHome.concat("/conf/flink-conf.yaml"));
      if (!yaml.exists()) {
        yaml = new File(this.flinkHome.concat("/conf/config.yaml"));
      }
      if (!yaml.exists()) {
        throw new ApiAlertException("cannot find config.yaml|flink-conf.yaml in flink/conf ");
      }
    } else {
      yaml = new File(this.flinkHome.concat("/conf/config.yaml"));
      if (!yaml.exists()) {
        throw new ApiAlertException("cannot find config.yaml in flink/conf ");
      }
    }
    try {
      String flinkConf = FileUtils.readFileToString(yaml, StandardCharsets.UTF_8);
      this.flinkConf = DeflaterUtils.zipString(flinkConf);
    } catch (Exception e) {
      throw new ApiDetailException(e);
    }
  }

  public void doSetVersion() {
    this.setVersion(this.getFlinkVersion().version());
    this.setScalaVersion(this.getFlinkVersion().scalaVersion());
    if (!streamParkScalaVersion.startsWith(this.getFlinkVersion().scalaVersion())) {
      throw new UnsupportedOperationException(
          String.format(
              "The current Scala version of StreamPark is %s, but the scala version of Flink to be added is %s, which does not match, Please check",
              streamParkScalaVersion, this.getFlinkVersion().scalaVersion()));
    }
  }

  public Map<String, String> convertFlinkYamlAsMap() {
    String flinkYamlString = DeflaterUtils.unzipString(flinkConf);
    return PropertiesUtils.loadFlinkConfYaml(flinkYamlString);
  }

  @JsonIgnore
  public Properties getFlinkConfig() {
    String flinkYamlString = DeflaterUtils.unzipString(flinkConf);
    Properties flinkConfig = new Properties();
    Map<String, String> config = PropertiesUtils.loadFlinkConfYaml(flinkYamlString);
    flinkConfig.putAll(config);
    return flinkConfig;
  }

  @JsonIgnore
  public FlinkVersion getFlinkVersion() {
    if (this.flinkVersion == null) {
      this.flinkVersion = new FlinkVersion(this.flinkHome);
    }
    return this.flinkVersion;
  }

  public void unzipFlinkConf() {
    this.flinkConf = DeflaterUtils.unzipString(this.flinkConf);
  }

  @JsonIgnore
  public String getLargeVersion() {
    return this.version.substring(0, this.version.lastIndexOf("."));
  }

  @JsonIgnore
  public String getVersionOfFirst() {
    return this.version.split("\\.")[0];
  }

  @JsonIgnore
  public String getVersionOfMiddle() {
    return this.version.split("\\.")[1];
  }

  @JsonIgnore
  public String getVersionOfLast() {
    return this.version.split("\\.")[2];
  }
}
