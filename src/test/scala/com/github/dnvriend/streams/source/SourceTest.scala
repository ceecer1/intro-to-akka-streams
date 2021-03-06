/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend.streams.source

import akka.stream.scaladsl.{ Concat, Merge, Sink, Source }
import com.github.dnvriend.streams.TestSpec

class SourceTest extends TestSpec {
  type Seq[A] = scala.collection.immutable.Seq[A]

  // Takes multiple streams and outputs one stream formed from the input streams
  // by first emitting all of the elements from the first stream and then emitting
  // all of the elements from the second stream, etc.
  it should "concat three sources" in {
    Source.combine(
      Source.single("Hello"),
      Source.repeat("World").take(5),
      Source.single("!")
    )(Concat(_)).runWith(Sink.seq)
      .futureValue shouldBe Seq("Hello", "World", "World", "World", "World", "World", "!")
  }

  // Merge several streams, taking elements as they arrive from input streams
  // picking randomly when several have elements ready
  it should "merge three sources" in {
    val xs = Source.combine(
      Source.single("Hello"),
      Source.repeat("World").take(5),
      Source.single("!")
    )(Merge(_)).runWith(Sink.seq)
      .futureValue
    xs should contain allOf ("Hello", "!", "World")
    xs.count(_ == "World") shouldBe 5
    xs.count(_ == "!") shouldBe 1
    xs.count(_ == "Hello") shouldBe 1
  }

  it should "emit a single element" in {
    Source.single("Foo")
      .recover { case t: Throwable ⇒ "Bar" }
      .runWith(Sink.seq).futureValue shouldBe Seq("Foo")
  }

  it should "recover from an exception after materialization" in {
    Source.fromIterator(() ⇒ Iterator(throw new RuntimeException("")))
      .recover { case t: Throwable ⇒ "Bar" }
      .runWith(Sink.seq).futureValue shouldBe Seq("Bar")
  }

  it should "recover a failed stream when exception in a stage" in {
    Source.single("Foo")
      .map { e ⇒ throw new RuntimeException(""); e }
      .recover { case t: Throwable ⇒ "Bar" }
      .runWith(Sink.seq).futureValue shouldBe Seq("Bar")
  }

  // cause, exception gets evaluated before materialization of the graph
  it should "not recover from an exception in the source" in {
    intercept[RuntimeException] { // thus an exception is thrown up the stack; the exception is not propagated
      Source.single(throw new RuntimeException(""))
        .recover { case t: Throwable ⇒ "Bar" }
        .runWith(Sink.seq).toTry should be a 'failure
    }
  }

  //  it should "do a map-reduce operation just like Hadoop or Spark" in {
  //    def substr(xs: Array[String]) =
  //      Source(xs.toList)
  //        .groupBy(1000, identity)
  //        .map(_ → 1)
  //        .reduce((l, r) ⇒ (l._1, l._2 + r._2))
  //        .mergeSubstreams
  //    //
  //    Source.single("Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, 'and what is the use of a book,' thought Alice 'without pictures or conversations?'\nSo she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her.\nThere was nothing so very remarkable in that; nor did Alice think it so very much out of the way to hear the Rabbit say to itself, 'Oh dear! Oh dear! I shall be late!' (when she thought it over afterwards, it occurred to her that she ought to have wondered at this, but at the time it all seemed quite natural); but when the Rabbit actually took a watch out of its waistcoat-pocket, and looked at it, and then hurried on, Alice started to her feet, for it flashed across her mind that she had never before seen a rabbit with either a waistcoat-pocket, or a watch to take out of it, and burning with curiosity, she ran across the field after it, and fortunately was just in time to see it pop down a large rabbit-hole under the hedge.\nIn another moment down went Alice after it, never once considering how in the world she was to get out again.\nThe rabbit-hole went straight on like a tunnel for some way, and then dipped suddenly down, so suddenly that Alice had not a moment to think about stopping herself before she found herself falling down a very deep well.\nEither the well was very deep, or she fell very slowly, for she had plenty of time as she went down to look about her and to wonder what was going to happen next. First, she tried to look down and make out what she was coming to, but it was too dark to see anything; then she looked at the sides of the well, and noticed that they were filled with cupboards and book-shelves; here and there she saw maps and pictures hung upon pegs. She took down a jar from one of the shelves as she passed; it was labelled 'ORANGE MARMALADE', but to her great disappointment it was empty: she did not like to drop the jar for fear of killing somebody, so managed to put it into one of the cupboards as she fell past it.\n'Well!' thought Alice to herself, 'after such a fall as this, I shall think nothing of tumbling down stairs! How brave they'll all think me at home! Why, I wouldn't say anything about it, even if I fell off the top of the house!' (Which was very likely true.)\nDown, down, down. Would the fall never come to an end! 'I wonder how many miles I've fallen by this time?' she said aloud. 'I must be getting somewhere near the centre of the earth. Let me see: that would be four thousand miles down, I think—' (for, you see, Alice had learnt several things of this sort in her lessons in the schoolroom, and though this was not a very good opportunity for showing off her knowledge, as there was no one to listen to her, still it was good practice to say it over) '—yes, that's about the right distance—but then I wonder what Latitude or Longitude I've got to?' (Alice had no idea what Latitude was, or Longitude either, but thought they were nice grand words to say.)\nPresently she began again. 'I wonder if I shall fall right through the earth! How funny it'll seem to come out among the people that walk with their heads downward! The Antipathies, I think—' (she was rather glad there was no one listening, this time, as it didn't sound at all the right word) '—but I shall have to ask them what the name of the country is, you know. Please, Ma'am, is this New Zealand or Australia?' (and she tried to curtsey as she spoke—fancy curtseying as you're falling through the air! Do you think you could manage it?) 'And what an ignorant little girl she'll think me for asking! No, it'll never do to ask: perhaps I shall see it written up somewhere.'")
  //      .map(_.split(" "))
  //      .flatMapConcat(xs ⇒ substr(xs))
  //      .filter { case (k, v) ⇒ k == "Alice" } // get Alice
  //      .runWith(Sink.seq)
  //      .futureValue shouldBe ""
  //  }
}