// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.platform.localstore

import org.mockito.MockitoSugar
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import java.sql.Connection

class DbDispatcherLeftOpsSpec extends AnyFreeSpec with MockitoSugar with Matchers {

  "rollbackOnLeft should rollback on left" in {
    val conn = mock[Connection]
    DbDispatcherLeftOps
      .rollbackOnLeft(_ => Left(""))(conn) shouldBe Left("")

    verify(conn, times(1)).rollback()
    verifyNoMoreInteractions(conn)
  }

  "rollbackOnLeft should not rollback on right" in {
    val conn = mock[Connection]
    DbDispatcherLeftOps
      .rollbackOnLeft(_ => Right(""))(conn) shouldBe Right("")

    verifyZeroInteractions(conn)
  }

}