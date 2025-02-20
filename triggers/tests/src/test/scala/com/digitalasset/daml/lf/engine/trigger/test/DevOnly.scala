// Copyright (c) 2022 Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: Apache-2.0

package com.daml.lf.engine.trigger.test

import akka.stream.scaladsl.Flow
import com.daml.lf.data.Ref._
import com.daml.ledger.api.testing.utils.SuiteResourceManagementAroundAll
import com.daml.ledger.api.v1.commands.CreateCommand
import com.daml.ledger.api.v1.event.{CreatedEvent, Event}
import com.daml.ledger.api.v1.event.Event.Event.Created
import com.daml.ledger.api.v1.value.{Identifier, Record, RecordField, Value}
import com.daml.ledger.api.v1.{value => LedgerApi}
import com.daml.lf.engine.trigger.Runner.TriggerContext
import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import com.daml.lf.engine.trigger.TriggerMsg
import com.daml.util.Ctx

import scala.collection.concurrent.TrieMap

class DevOnly
    extends AsyncWordSpec
    with AbstractTriggerTest
    with Matchers
    with Inside
    with SuiteResourceManagementAroundAll
    with TryValues {
  self: Suite =>

  import DevOnly._

  this.getClass.getSimpleName can {
    "InterfaceTest" should {
      val triggerId = QualifiedName.assertFromString("Interface:test")
      val tId = LedgerApi.Identifier(packageId, "Interface", "Asset")
      "1 transfer" in {
        for {
          client <- ledgerClient()
          party <- allocateParty(client)
          runner = getRunner(client, triggerId, party)
          (acs, offset) <- runner.queryACS()
          // 1 for create of Asset
          // 1 for completion
          // 1 for exercise
          // 1 for corresponding completion
          _ <- runner.runWithACS(acs, offset, msgFlow = Flow[TriggerContext[TriggerMsg]].take(4))._2
          acs <- queryACS(client, party)
        } yield {
          acs(tId) should have length 1
        }
      }
    }

    "trigger runner" should {
      val templateA = Identifier(packageId, "InterfaceTriggers", "A")
      val templateB = Identifier(packageId, "InterfaceTriggers", "B")
      val visibleViaAllInDar = "visible via 'AllInDar'"
      val visibleViaInterfaceI = "visible via 'registeredTemplate @I'"
      val visibleViaTemplateA = "visible via 'registeredTemplate @A'"

      "succeed with all templates and interfaces registered" in {
        val triggerId = QualifiedName.assertFromString("InterfaceTriggers:globalTrigger")
        val transactionEvents = TrieMap.empty[String, Seq[Event]]

        for {
          client <- ledgerClient()
          party <- allocateParty(client)
          runner = getRunner(client, triggerId, party)

          // Determine current ledger offset
          queryResult <- runner.queryACS()
          (_, offset) = queryResult

          _ <- create(
            client,
            party,
            CreateCommand(
              templateId = Some(templateA),
              createArguments = Some(
                Record(
                  fields = Seq(
                    RecordField("owner", Some(Value().withParty(party))),
                    RecordField("tag", Some(Value().withText(visibleViaAllInDar))),
                  )
                )
              ),
            ),
          )
          _ <- create(
            client,
            party,
            CreateCommand(
              templateId = Some(templateB),
              createArguments = Some(
                Record(
                  fields = Seq(
                    RecordField("owner", Some(Value().withParty(party))),
                    RecordField("tag", Some(Value().withText(visibleViaAllInDar))),
                  )
                )
              ),
            ),
          )

          // Determine ACS for this test run's setup
          queryResult <- runner.queryACS()
          (acs, _) = queryResult

          // 1 for ledger create command completion
          // 1 for ledger create command completion
          // 1 for transactional create of template A
          // 1 for transactional create of template B
          _ <- runner
            .runWithACS(
              acs,
              offset,
              msgFlow = Flow[TriggerContext[TriggerMsg]]
                .wireTap {
                  case Ctx(_, msg: TriggerMsg.Transaction, _) =>
                    transactionEvents.addOne(msg.t.transactionId -> msg.t.events)
                  case _ =>
                  // No evidence to collect
                }
                .take(4),
            )
            ._2
        } yield {
          transactionEvents.size shouldBe 2
          val Seq(templateATransactionId, templateBTransactionId) =
            transactionEvents.keys.toSeq.sorted
          transactionEvents(templateATransactionId) shouldHaveCreateArgumentsFor templateA
          transactionEvents(templateATransactionId) shouldHaveViewValues (0, visibleViaAllInDar)
          transactionEvents(templateBTransactionId) shouldHaveCreateArgumentsFor templateB
          transactionEvents(templateBTransactionId) shouldHaveViewValues (1, visibleViaAllInDar)
        }
      }

      "succeed with interface registration and implementing template not registered" in {
        val triggerId = QualifiedName.assertFromString("InterfaceTriggers:triggerWithRegistration")
        val transactionEvents = TrieMap.empty[String, Seq[Event]]

        for {
          client <- ledgerClient()
          party <- allocateParty(client)
          runner = getRunner(client, triggerId, party)

          // Determine current ledger offset
          queryResult <- runner.queryACS()
          (_, offset) = queryResult

          _ <- create(
            client,
            party,
            CreateCommand(
              templateId = Some(templateA),
              createArguments = Some(
                Record(
                  fields = Seq(
                    RecordField("owner", Some(Value().withParty(party))),
                    RecordField(
                      "tag",
                      Some(Value().withText(visibleViaTemplateA)),
                    ),
                  )
                )
              ),
            ),
          )
          _ <- create(
            client,
            party,
            CreateCommand(
              templateId = Some(templateB),
              createArguments = Some(
                Record(
                  fields = Seq(
                    RecordField("owner", Some(Value().withParty(party))),
                    RecordField(
                      "tag",
                      Some(Value().withText(visibleViaInterfaceI)),
                    ),
                  )
                )
              ),
            ),
          )

          // Determine ACS for this test run's setup
          queryResult <- runner.queryACS()
          (acs, _) = queryResult

          // 1 for ledger create command completion
          // 1 for ledger create command completion
          // 1 for transactional create of template A
          // 1 for transactional create of template B, via interface I
          _ <- runner
            .runWithACS(
              acs,
              offset,
              msgFlow = Flow[TriggerContext[TriggerMsg]]
                .wireTap {
                  case Ctx(_, msg: TriggerMsg.Transaction, _) =>
                    transactionEvents.addOne(msg.t.transactionId -> msg.t.events)
                  case _ =>
                  // No evidence to collect
                }
                .take(4),
            )
            ._2
        } yield {
          transactionEvents.size shouldBe 2
          val Seq(templateATransactionId, templateBTransactionId) =
            transactionEvents.keys.toSeq.sorted
          transactionEvents(templateATransactionId) shouldHaveCreateArgumentsFor templateA
          transactionEvents(templateATransactionId) shouldHaveViewValues (0, visibleViaTemplateA)
          transactionEvents(templateBTransactionId) shouldHaveNoCreateArgumentsFor templateB
          transactionEvents(templateBTransactionId) shouldHaveViewValues (1, visibleViaInterfaceI)
        }
      }

      "succeed with interface only registrations" in {
        val triggerId = QualifiedName.assertFromString("InterfaceTriggers:interfaceOnlyTrigger")
        val transactionEvents = TrieMap.empty[String, Seq[Event]]

        for {
          client <- ledgerClient()
          party <- allocateParty(client)
          runner = getRunner(client, triggerId, party)

          // Determine current ledger offset
          queryResult <- runner.queryACS()
          (_, offset) = queryResult

          _ <- create(
            client,
            party,
            CreateCommand(
              templateId = Some(templateA),
              createArguments = Some(
                Record(
                  fields = Seq(
                    RecordField("owner", Some(Value().withParty(party))),
                    RecordField(
                      "tag",
                      Some(Value().withText(visibleViaInterfaceI)),
                    ),
                  )
                )
              ),
            ),
          )
          _ <- create(
            client,
            party,
            CreateCommand(
              templateId = Some(templateB),
              createArguments = Some(
                Record(
                  fields = Seq(
                    RecordField("owner", Some(Value().withParty(party))),
                    RecordField(
                      "tag",
                      Some(Value().withText(visibleViaInterfaceI)),
                    ),
                  )
                )
              ),
            ),
          )

          // Determine ACS for this test run's setup
          queryResult <- runner.queryACS()
          (acs, _) = queryResult

          // 1 for ledger create command completion
          // 1 for ledger create command completion
          // 1 for transactional create of template A, via interface I
          // 1 for transactional create of template B, via interface I
          _ <- runner
            .runWithACS(
              acs,
              offset,
              msgFlow = Flow[TriggerContext[TriggerMsg]]
                .wireTap {
                  case Ctx(_, msg: TriggerMsg.Transaction, _) =>
                    transactionEvents.addOne(msg.t.transactionId -> msg.t.events)
                  case _ =>
                  // No evidence to collect
                }
                .take(4),
            )
            ._2
        } yield {
          transactionEvents.size shouldBe 2
          val Seq(templateATransactionId, templateBTransactionId) =
            transactionEvents.keys.toSeq.sorted
          transactionEvents(templateATransactionId) shouldHaveNoCreateArgumentsFor templateA
          transactionEvents(templateATransactionId) shouldHaveViewValues (0, visibleViaInterfaceI)
          transactionEvents(templateBTransactionId) shouldHaveNoCreateArgumentsFor templateB
          transactionEvents(templateBTransactionId) shouldHaveViewValues (1, visibleViaInterfaceI)
        }
      }
    }
  }
}

object DevOnly extends Matchers with Inside {

  implicit class TriggerMsgTestHelper(events: Seq[Event]) {
    def shouldHaveNoCreateArgumentsFor(templateId: Identifier): Assertion =
      inside(events) {
        case Seq(
              Event(
                Created(CreatedEvent(_, _, Some(`templateId`), _, None, _, Seq(_), _, _, _, _, _))
              )
            ) =>
          succeed
      }

    def shouldHaveCreateArgumentsFor(templateId: Identifier): Assertion =
      inside(events) {
        case Seq(
              Event(
                Created(
                  CreatedEvent(_, _, Some(`templateId`), _, Some(_), _, Seq(_), _, _, _, _, _)
                )
              )
            ) =>
          succeed
      }

    def shouldHaveViewValues(n: Int, label: String): Assertion =
      inside(events) {
        case Seq(
              Event(
                Created(
                  CreatedEvent(_, _, _, _, _, _, Seq(view), _, _, _, _, _)
                )
              )
            ) =>
          inside(view.getViewValue) {
            case Record(_, Seq(RecordField(_, Some(actualN)), RecordField(_, Some(actualLabel)))) =>
              actualN shouldBe Value().withInt64(n.toLong)
              actualLabel shouldBe Value().withText(label)
          }
      }
  }
}
