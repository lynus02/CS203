import React, {useState, useRef, useEffect} from "react";
import {Button} from "./ui/button";
import {Card} from "./ui/card";
import {Input} from "./ui/input";
import {MessageCircle, X, Send, Bot, User} from "lucide-react";

interface Message {
    id: string;
    text: string;
    sender: "user" | "bot";
    timestamp: Date;
}

export function AIChat() {
    const [isOpen, setIsOpen] = useState(false);
    const [messages, setMessages] = useState<Message[]>([
        {
            id: "1",
            text: "Hi! I'm your FoodTariff assistant. I can help you with:\n\n• Understanding tariff calculations\n• Finding HS codes for food products\n• Explaining trade agreements\n• Shipping cost estimates\n\nHow can I help you today?",
            sender: "bot",
            timestamp: new Date(),
        },
    ]);
    const [inputValue, setInputValue] = useState("");
    const [isTyping, setIsTyping] = useState(false);
    const messagesEndRef = useRef<HTMLDivElement>(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({behavior: "smooth"});
    };

    useEffect(() => {
        scrollToBottom();
    }, [messages]);

    const handleWheel = (e: React.WheelEvent<HTMLDivElement>) => {
        const el = e.currentTarget;
        const atTop = el.scrollTop <= 0;
        const atBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 1;

        // only block scrolling if we truly can’t move further
        if ((atTop && e.deltaY < 0) || (atBottom && e.deltaY > 0)) {
            e.stopPropagation(); // stops bubbling to page
            e.preventDefault();  // blocks page scroll
        }
    };


    const handleSendMessage = async () => {
        if (!inputValue.trim()) return;

        const userMessage: Message = {
            id: Date.now().toString(),
            text: inputValue,
            sender: "user",
            timestamp: new Date(),
        };

        setMessages((prev) => [...prev, userMessage]);
        setInputValue("");
        setIsTyping(true);

        try {
            const token = localStorage.getItem("token");
            const response = await fetch("http://localhost:8080/api/chatbot", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    ...(token ? {Authorization: `Bearer ${token}`} : {}),
                },
                body: JSON.stringify({prompt: inputValue}),
            });

            if (!response.ok) throw new Error("Failed to fetch AI response");
            const data = await response.json();

            const botResponse: Message = {
                id: (Date.now() + 1).toString(),
                text: data.answer,
                sender: "bot",
                timestamp: new Date(),
            };

            setMessages((prev) => [...prev, botResponse]);
        } catch (error) {
            console.error("Chat error:", error);
            setMessages((prev) => [
                ...prev,
                {
                    id: (Date.now() + 2).toString(),
                    text: "Sorry, I'm having trouble connecting to the AI server.",
                    sender: "bot",
                    timestamp: new Date(),
                },
            ]);
        } finally {
            setIsTyping(false);
        }
    };

    const handleKeyPress = (e: React.KeyboardEvent) => {
        if (e.key === "Enter" && !e.shiftKey) {
            e.preventDefault();
            handleSendMessage();
        }
    };

    return (
        <div
            id="ai-chat-root"
            style={{
                position: "fixed",   // prevents stacking context override
                bottom: "24px",
                right: "24px",
                zIndex: 9999999,
            }}
            className="pointer-events-auto flex flex-col items-end max-w-[420px]"
        >

            {/* Chat Window */}
            {isOpen && (
                <Card
                    cclassName="
                        !w-auto         /* override Shadcn's w-full */
                        w-[95vw] sm:w-[50vw] lg:w-[33vw]
                        max-w-[420px] min-w-[300px]
                        flex flex-col shadow-2xl border border-border bg-card
                        h-[500px]
                        overflow-hidden rounded-xl pointer-events-auto
                      ">

                    {/* Header */}
                    <div
                        className="flex-shrink-0 flex items-center justify-between p-4 border-b bg-primary text-primary-foreground">
                        <div className="flex items-center gap-2">
                            <div
                                className="h-8 w-8 rounded-full bg-primary-foreground/20 flex items-center justify-center">
                                <Bot className="h-5 w-5"/>
                            </div>
                            <div>
                                <div className="font-medium">FoodTariff Assistant</div>
                                <div className="text-xs opacity-80">Online • Ready to help</div>
                            </div>
                        </div>
                        <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => setIsOpen(false)}
                            className="h-8 w-8 text-primary-foreground hover:bg-primary-foreground/20"
                        >
                            <X className="h-4 w-4"/>
                        </Button>
                    </div>

                    {/* Scrollable messages area */}
                    <div
                        className="flex-1 min-h-0 p-4 overflow-y-auto overscroll-contain scrollbar-none"
                        onMouseEnter={() => {
                            document.body.style.overflow = "hidden";
                        }}
                        onMouseLeave={() => {
                            document.body.style.overflow = "";
                        }}
                        onWheel={(e) => {
                            const el = e.currentTarget;
                            const atTop = el.scrollTop <= 0;
                            const atBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 1;

                            if ((atTop && e.deltaY < 0) || (atBottom && e.deltaY > 0)) {
                                e.stopPropagation();
                                e.preventDefault();
                            }
                        }}
                        style={{
                            WebkitOverflowScrolling: "touch",
                        }}
                    >
                        <div className="space-y-4">
                            {messages.map((message) => (
                                <div
                                    key={message.id}
                                    className={`flex gap-2 ${
                                        message.sender === "user" ? "flex-row-reverse" : "flex-row"
                                    }`}
                                >
                                    <div
                                        className={`h-8 w-8 rounded-full flex items-center justify-center ${
                                            message.sender === "user"
                                                ? "bg-primary text-primary-foreground"
                                                : "bg-muted"
                                        }`}
                                    >
                                        {message.sender === "user" ? (
                                            <User className="h-4 w-4"/>
                                        ) : (
                                            <Bot className="h-4 w-4"/>
                                        )}
                                    </div>
                                    <div
                                        className={`rounded-lg px-4 py-2 max-w-[260px] ${
                                            message.sender === "user"
                                                ? "bg-primary text-primary-foreground"
                                                : "bg-muted"
                                        }`}
                                    >
                                        <div className="text-sm whitespace-pre-line">{message.text}</div>
                                        <div
                                            className={`text-xs mt-1 ${
                                                message.sender === "user"
                                                    ? "text-primary-foreground/70"
                                                    : "text-muted-foreground"
                                            }`}
                                        >
                                            {message.timestamp.toLocaleTimeString([], {
                                                hour: "2-digit",
                                                minute: "2-digit",
                                            })}
                                        </div>
                                    </div>
                                </div>
                            ))}
                            {isTyping && (
                                <div className="flex gap-2">
                                    <div className="h-8 w-8 rounded-full bg-muted flex items-center justify-center">
                                        <Bot className="h-4 w-4"/>
                                    </div>
                                    <div className="rounded-lg px-4 py-2 bg-muted">
                                        <div className="flex gap-1">
                                            <div
                                                className="w-2 h-2 rounded-full bg-muted-foreground/40 animate-bounce"/>
                                            <div
                                                className="w-2 h-2 rounded-full bg-muted-foreground/40 animate-bounce"
                                                style={{animationDelay: "150ms"}}
                                            />
                                            <div
                                                className="w-2 h-2 rounded-full bg-muted-foreground/40 animate-bounce"
                                                style={{animationDelay: "300ms"}}
                                            />
                                        </div>
                                    </div>
                                </div>
                            )}
                            <div ref={messagesEndRef}/>
                        </div>
                    </div>


                    {/* Input Area */}
                    <div className="flex-shrink-0 p-3 border-t bg-background">
                        <div className="flex gap-2">
                            <Input
                                placeholder="Ask me anything about tariffs..."
                                value={inputValue}
                                onChange={(e) => setInputValue(e.target.value)}
                                onKeyPress={handleKeyPress}
                                className="flex-1"
                            />
                            <Button
                                onClick={handleSendMessage}
                                size="icon"
                                disabled={!inputValue.trim()}
                            >
                                <Send className="h-4 w-4"/>
                            </Button>
                        </div>
                    </div>
                </Card>
            )}

            {/* Floating Toggle Button */}
            {!isOpen && (
                <Button
                    onClick={() => setIsOpen(true)}
                    size="icon"
                    className="
                        pointer-events-auto
                        h-14 w-14
                        rounded-full
                        shadow-xl hover:scale-110 transition-transform
                        bg-primary text-primary-foreground
                        flex items-center justify-center
                    "
                >
                    <MessageCircle className="h-6 w-6"/>
                </Button>
            )}
        </div>
    );
}
