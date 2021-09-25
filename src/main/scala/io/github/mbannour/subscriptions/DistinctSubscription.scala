package io.github.mbannour.subscriptions

import com.mongodb.client.model.Collation
import com.mongodb.reactivestreams.client.DistinctPublisher
import org.bson.conversions.Bson
import org.reactivestreams.{Subscription => JSubscription, Subscriber => JSubscriber}
import zio.IO

import java.util.concurrent.TimeUnit
import scala.collection.mutable.ArrayBuffer

case class DistinctSubscription[T](p: DistinctPublisher[T]) extends Subscription[Iterable[T]] {

  override def subscribe[_]: IO[Throwable, Iterable[T]] =
    IO.async[Throwable, Iterable[T]] { callback =>
      p.subscribe {
        new JSubscriber[T] {
          val items = new ArrayBuffer[T]()

          override def onSubscribe(s: JSubscription): Unit = s.request(Long.MaxValue)

          override def onNext(t: T): Unit = items += t

          override def onError(t: Throwable): Unit = callback(IO.fail(t))

          override def onComplete(): Unit = callback(IO.succeed(items))
        }
      }
    }

   def filter(filter: Bson): DistinctSubscription[T] = this.copy(p.filter(filter))

  def maxTime(maxTime: Long, timeUnit: TimeUnit): DistinctSubscription[T] = this.copy(p.maxTime(maxTime, timeUnit))

  def collation(collation: Collation): DistinctSubscription[T] = this.copy(p.collation(collation))

  def batchSize(batchSize: Int): DistinctSubscription[T] = this.copy(p.batchSize(batchSize))

  def first(): SingleItemSubscription[T] = SingleItemSubscription(p.first())

}
