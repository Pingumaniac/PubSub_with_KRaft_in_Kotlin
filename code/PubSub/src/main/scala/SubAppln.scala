object SubAppln {
  def subscribe(topic: String, callback: (Any) => Unit): Unit = {
    // Subscribe to a topic and define a callback
  }
  
  def unsubscribe(topic: String): Unit = {
    // Unsubscribe from a specific topic
  }
  
  def disconnect(): Unit = {
    // Unsubscribe from the current subscription
  }
  
  def change_subscription(topics: Set[String]): Unit = {
    // Change the set of subscribed topics
  }
  
  def handle_listen_errors(error: Throwable): Unit = {
    // Handle errors during message listening
  }
}
