/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.benchmarks

import squareup.wire.benchmarks.EmailMessage as EmailMessageWire
import squareup.wire.benchmarks.EmailMessageProto.EmailMessage as EmailMessageProtobuf
import squareup.wire.benchmarks.EmailSearchResponse as EmailSearchResponseWire
import squareup.wire.benchmarks.EmailSearchResponseProto.EmailSearchResponse as EmailSearchResponseProtobuf
import squareup.wire.benchmarks.EmailThread as EmailThreadWire
import squareup.wire.benchmarks.EmailThreadProto.EmailThread as EmailThreadProtobuf
import squareup.wire.benchmarks.NameAndAddress as NameAndAddressWire
import squareup.wire.benchmarks.NameAndAddressProto.NameAndAddress as NameAndAddressProtobuf

object SampleData {
  private val sentAt = 1627165590000L
  private val senderDisplayName = "Jesse Wilson"
  private val senderEmailAddress = "jesse@example.com"
  private val recipientDisplayName = "Waterloo Dentist"
  private val recipientEmailAddress = "dentist@example.com"
  private val subject = "Re: Appt. Confirmation"
  private val body = """
      |Hey Sandy!
      |
      |Yeah thanks for checking. I'll see you Monday at 9:30!
      |
      |– Jesse 🤙🏻
      |
      |> On Thursday, July 22, 2021, at 8:01 AM, dentist@example.com wrote:
      |>
      |> 23-JULY-2021
      |>
      |> Dear Mr. Wilson,
      |>
      |> This is confirmation of your eye appointment for: MONDAY JULY 26TH@ 9:30A.M
      |>
      |> Please give us a quick call or email us to confirm you will be attending this appointment.
      |>
      |> We look forward to seeing you.
      |>
      |>
      |> Sincerely,
      |>
      |> Sandy Winchester
      |>
      |> Waterloo Dentist Office
      |>
      |> (519) 555-2233
      |> dentist@example.com
      |>
      |
  """.trimMargin()

  @JvmStatic
  fun newMediumValueWire(): EmailSearchResponseWire {
    return EmailSearchResponseWire(
      query = recipientDisplayName,
      results = listOf(
        EmailThreadWire(
          subject = subject,
          messages = listOf(newEmailMessageWire()),
        ),
      ),
    )
  }

  private fun newEmailMessageWire() = EmailMessageWire(
    sent_at = sentAt,
    sender = newSenderWire(),
    recipients = listOf(newRecipientWire()),
    subject = subject,
    body = body,
  )

  fun newSenderWire() = NameAndAddressWire(
    display_name = senderDisplayName,
    email_address = senderEmailAddress,
  )

  fun newRecipientWire() = NameAndAddressWire(
    display_name = recipientDisplayName,
    email_address = recipientEmailAddress,
  )

  @JvmStatic
  fun newMediumValueProtobuf(): EmailSearchResponseProtobuf {
    return EmailSearchResponseProtobuf.newBuilder()
      .apply {
        query = recipientDisplayName
        addResults(
          EmailThreadProtobuf.newBuilder()
            .apply {
              subject = SampleData.subject
              addMessages(newEmailMessageProtobuf())
            }.build(),
        )
      }.build()
  }

  private fun newEmailMessageProtobuf() = EmailMessageProtobuf.newBuilder()
    .apply {
      sentAt = SampleData.sentAt
      sender = newSenderProtobuf()
      addRecipients(newRecipientProtobuf())
      subject = SampleData.subject
      body = SampleData.body
    }.build()

  private fun newSenderProtobuf() = NameAndAddressProtobuf.newBuilder()
    .apply {
      displayName = senderDisplayName
      emailAddress = senderEmailAddress
    }.build()

  private fun newRecipientProtobuf() = NameAndAddressProtobuf.newBuilder()
    .apply {
      displayName = recipientDisplayName
      emailAddress = recipientEmailAddress
    }.build()
}
