// Copyright (c) 2021 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.platform.store

import com.daml.ledger.api.domain
import com.daml.ledger.participant.state.{v1 => state}
import com.daml.platform.store.Conversions._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class ConversionsSpec extends AsyncWordSpec with Matchers {
  "converting rejection reasons" should {
    "convert an 'Inconsistent' rejection reason" in {
      val reason = domain.RejectionReason.Inconsistent("This was not very consistent.")
      val converted = reason.toParticipantStateRejectionReason
      converted should be(state.RejectionReasonV0.Inconsistent("This was not very consistent."))
    }

    "convert an 'Disputed' rejection reason" in {
      val reason = domain.RejectionReason.Disputed("I dispute that.")
      val converted = reason.toParticipantStateRejectionReason
      converted should be(state.RejectionReasonV0.Disputed("I dispute that."))
    }

    "convert an 'OutOfQuota' rejection reason" in {
      val reason = domain.RejectionReason.OutOfQuota("Insert coins to continue.")
      val converted = reason.toParticipantStateRejectionReason
      converted should be(state.RejectionReasonV0.ResourcesExhausted("Insert coins to continue."))
    }

    "convert an 'PartyNotKnownOnLedger' rejection reason" in {
      val reason = domain.RejectionReason.PartyNotKnownOnLedger("Who on earth is Alice?")
      val converted = reason.toParticipantStateRejectionReason
      converted should be(state.RejectionReasonV0.PartyNotKnownOnLedger("Who on earth is Alice?"))
    }

    "convert an 'SubmitterCannotActViaParticipant' rejection reason" in {
      val reason = domain.RejectionReason.SubmitterCannotActViaParticipant("Wrong box.")
      val converted = reason.toParticipantStateRejectionReason
      converted should be(state.RejectionReasonV0.SubmitterCannotActViaParticipant("Wrong box."))
    }

    "convert an 'InvalidLedgerTime' rejection reason" in {
      val reason = domain.RejectionReason.InvalidLedgerTime("Too late.")
      val converted = reason.toParticipantStateRejectionReason
      converted should be(state.RejectionReasonV0.InvalidLedgerTime("Too late."))
    }
  }
}