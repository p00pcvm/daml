// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.metrics

import com.codahale.metrics.MetricRegistry
import com.daml.metrics.api.dropwizard.FactoryWithDBMetrics
import com.daml.metrics.api.{MetricDoc, MetricName}

@MetricDoc.GroupTag(
  representative = "daml.identity_provider_config_store.<operation>",
  groupableClass = classOf[DatabaseMetrics],
)
class IdentityProviderConfigStoreMetrics(
    override val prefix: MetricName,
    override val registry: MetricRegistry,
) extends FactoryWithDBMetrics {

  val cacheByIssuer = new CacheMetrics(prefix :+ "cache_by_issuer", registry)
  val createIdpConfig: DatabaseMetrics = createDbMetrics("create_identity_provider_config")
  val getIdpConfig: DatabaseMetrics = createDbMetrics("get_identity_provider_config")
  val deleteIdpConfig: DatabaseMetrics = createDbMetrics("delete_identity_provider_config")
  val updateIdpConfig: DatabaseMetrics = createDbMetrics("update_identity_provider_config")
  val listIdpConfigs: DatabaseMetrics = createDbMetrics("list_identity_provider_configs")
}
