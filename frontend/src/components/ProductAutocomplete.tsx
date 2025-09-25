@ -0,0 +1,79 @@
import { useState, useRef } from "react";
import { fetchProductSuggestions } from "../Api";
import { Popover, PopoverContent, PopoverTrigger } from "./ui/popover";
import { Button } from "./ui/button";
import { Command, CommandInput, CommandList, CommandItem, CommandEmpty } from "./ui/command";
import { Search } from "lucide-react";

export function ProductAutocomplete({ value, onChange }) {
    const [input, setInput] = useState("");
    const [open, setOpen] = useState(false);
    const [suggestions, setSuggestions] = useState([]);
    const debounceRef = useRef();

    // Debounced fetch
    const handleInputChange = (val) => {
        setInput(val);
        clearTimeout(debounceRef.current);
        if (val.length > 1) {
            debounceRef.current = setTimeout(async () => {
                const results = await fetchProductSuggestions(val);
                setSuggestions(results);
            }, 250);
        } else {
            setSuggestions([]);
        }
    };

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <Button
                    variant="outline"
                    role="combobox"
                    aria-expanded={open}
                    className="w-full justify-between"
                >
                    {value
                        ? `${value.hsDescription} (${value.productCode6})`
                        : "Search products by name or HS code..."}
                    <Search className="ml-2 h-4 w-4 shrink-0 opacity-50" />
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-full p-0">
                <Command>
                    <CommandInput
                        placeholder="Search products..."
                        value={input}
                        onValueChange={handleInputChange}
                        autoFocus
                    />
                    <CommandList>
                        {suggestions.length === 0 ? (
                            <CommandEmpty>No products found.</CommandEmpty>
                        ) : (
                            suggestions.map((item) => (
                                <CommandItem
                                    key={item.trade_id}
                                    value={`${item.hsDescription} ${item.productCode6}`}
                                    onSelect={() => {
                                        onChange(item);
                                        setOpen(false);
                                        setInput("");
                                    }}
                                >
                                    <div className="flex flex-col">
                                        <div className="font-medium">{item.hsDescription}</div>
                                        <div className="text-xs text-muted-foreground">
                                            HS: {item.productCode6}
                                        </div>
                                    </div>
                                </CommandItem>
                            ))
                        )}
                    </CommandList>
                </Command>
            </PopoverContent>
        </Popover>
    );
}