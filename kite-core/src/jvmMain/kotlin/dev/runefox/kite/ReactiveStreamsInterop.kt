package dev.runefox.kite

import java.util.concurrent.Flow.*

private class EmitterPublisher<T>(val emitter: Emitter<T>) : Publisher<T> {
    override fun subscribe(subscriber: Subscriber<in T>) {
        emitter.subscribe(SubscriberReceiver(subscriber))
    }
}

private class ReceiverSubscriber<T>(val receiver: Receiver<T>) : Subscriber<T> {
    override fun onSubscribe(subscription: Subscription) {
        receiver.open(SubscriptionPipe(subscription))
    }

    override fun onNext(item: T) {
        receiver.receive(item)
    }

    override fun onError(throwable: Throwable) {
        receiver.error(throwable)
    }

    override fun onComplete() {
        receiver.complete()
    }
}

private class PipeSubscription(val pipe: Pipe) : Subscription {
    override fun request(n: Long) {
        pipe.request(n)
    }

    override fun cancel() {
        pipe.close()
    }
}

private class PublisherEmitter<T>(val publisher: Publisher<T>) : Emitter<T> {
    override fun subscribe(receiver: Receiver<T>) {
        publisher.subscribe(ReceiverSubscriber(receiver))
    }
}

private class SubscriberReceiver<T>(val subscriber: Subscriber<T>) : Receiver<T> {
    override fun open(pipe: Pipe) {
        subscriber.onSubscribe(PipeSubscription(pipe))
    }

    override fun receive(item: T) {
        subscriber.onNext(item)
    }

    override fun complete() {
        subscriber.onComplete()
    }

    override fun error(error: Throwable) {
        subscriber.onError(error)
    }
}

private class SubscriptionPipe(val subscription: Subscription) : Pipe {
    override fun request(n: Long) {
        if (n != 0L) {
            subscription.request(if (n < 0) Long.MAX_VALUE else n)
        }
    }

    override fun close() {
        subscription.cancel()
    }
}

fun <T> Emitter<T>.asPublisher(): Publisher<T> {
    return EmitterPublisher(this)
}

fun <T> Publisher<T>.asEmitter(): Emitter<T> {
    return PublisherEmitter(this)
}

fun <T> Receiver<T>.asSubscriber(): Subscriber<T> {
    return ReceiverSubscriber(this)
}

fun <T> Subscriber<T>.asReceiver(): Receiver<T> {
    return SubscriberReceiver(this)
}

fun <T> Publisher<T>.subscribe(receiver: Receiver<T>) {
    subscribe(receiver.asSubscriber())
}

fun <T> Emitter<T>.subscribe(subscriber: Subscriber<T>) {
    asPublisher().subscribe(subscriber)
}