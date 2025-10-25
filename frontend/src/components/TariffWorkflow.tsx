// import {useEffect, useState} from "react";
// import type { ComponentType } from "react";
// import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
// import { Button } from "./ui/button";
// import { Badge } from "./ui/badge";
// import { Progress } from "./ui/progress";
// import { CheckCircle, Circle, ArrowRight, Package, Calculator, Ship, DollarSign, FileText } from "lucide-react";
// import { Separator } from "./ui/separator";
// import {CustomsDutyCalculator} from "./CustomsDutyCalculator";
// import {TotalLandedCostCalculator} from "./TotalLandedCostCalculator";
//
// interface WorkflowStep {
//   id: string;
//   title: string;
//   description: string;
//   icon: React.ComponentType<{ className?: string }>;
//   completed: boolean;
//   active: boolean;
//   data?: any;
// }
//
// interface WorkflowData {
//   product?: {
//     name: string;
//     hsCode: string;
//     value: number;
//     quantity: number;
//   };
//   origin?: {
//     country: string;
//     manufacturer: string;
//   };
//   destination?: {
//     country: string;
//   };
//   shipping?: {
//     method: string;
//     cost: number;
//   };
//   duties?: {
//     rate: number;
//     amount: number;
//   };
//   certificate?: {
//     uploaded: boolean;
//     validated: boolean;
//   };
//   totalCost?: number;
// }
//
// export function TariffWorkflow() {
//   const [currentStep, setCurrentStep] = useState(0);
//   const [workflowData, setWorkflowData] = useState<WorkflowData>({});
//   const [customsResult, setCustomsResult] = useState<any>(null);
//
//     const handleCustomsResult = (result: any) => {
//         console.log("✅ Received customs result from calculator:", result);
//
//         setCustomsResult(result);
//
//         setWorkflowData(prev => ({
//             ...prev,
//             product: {
//                 ...(prev.product || {}),
//                 value: result.productValue,
//             },
//             duties: {
//                 rate: result.finalTariffRate,
//                 amount: result.dutyAmount,
//             },
//         }));
//     };
//
//
//     useEffect(() => {
//         if (workflowData.product && workflowData.shipping && customsResult) {
//             const productCost =
//                 (workflowData.product.value || 0) * (workflowData.product.quantity || 1);
//             const shippingCost = workflowData.shipping.cost || 0;
//             const dutyCost = customsResult.dutyAmount || 0;
//
//             const totalCost = productCost + shippingCost + dutyCost;
//
//             setWorkflowData((prev) => ({
//                 ...prev,
//                 totalCost,
//             }));
//         }
//     }, [workflowData.product, workflowData.shipping, customsResult]);
//
//     const steps: WorkflowStep[] = [
//     {
//       id: "product",
//       title: "Select Product",
//       description: "Choose your product and specify details",
//       icon: Package,
//       completed: !!workflowData.product,
//       active: currentStep === 0
//     },
//     {
//       id: "origin",
//       title: "Country of Origin",
//       description: "Specify manufacturing location",
//       icon: Circle,
//       completed: !!workflowData.origin,
//       active: currentStep === 1
//     },
//     {
//       id: "shipping",
//       title: "Shipping Details",
//       description: "Calculate shipping costs",
//       icon: Ship,
//       completed: !!workflowData.shipping,
//       active: currentStep === 2
//     },
//     {
//       id: "duties",
//       title: "Calculate Duties",
//       description: "Determine tariff rates and duties",
//       icon: Calculator,
//       completed: !!workflowData.duties,
//       active: currentStep === 3
//     },
//     {
//       id: "certificate",
//       title: "Certificate of Origin",
//       description: "Upload and validate certificates",
//       icon: FileText,
//       completed: !!workflowData.certificate?.validated,
//       active: currentStep === 4
//     },
//     {
//       id: "summary",
//       title: "Final Summary",
//       description: "Review total landed cost",
//       icon: DollarSign,
//       completed: !!workflowData.totalCost,
//       active: currentStep === 5
//     }
//   ];
//
//   const progress = (steps.filter(step => step.completed).length / steps.length) * 100;
//
//   const nextStep = () => {
//     if (currentStep < steps.length - 1) {
//       setCurrentStep(currentStep + 1);
//     }
//   };
//
//   const prevStep = () => {
//     if (currentStep > 0) {
//       setCurrentStep(currentStep - 1);
//     }
//   };
//
//   const goToStep = (stepIndex: number) => {
//     setCurrentStep(stepIndex);
//   };
//
//   const calculateFinalCost = () => {
//     const productCost = (workflowData.product?.value || 0) * (workflowData.product?.quantity || 1);
//     const shippingCost = workflowData.shipping?.cost || 0;
//     const dutyCost = workflowData.duties?.amount || 0;
//     const totalCost = productCost + shippingCost + dutyCost;
//
//     setWorkflowData(prev => ({
//       ...prev,
//       totalCost
//     }));
//   };
//
//   return (
//     <Card>
//       <CardHeader>
//         <CardTitle>Tariff Calculation Workflow</CardTitle>
//         <CardDescription>
//           Follow the step-by-step process to calculate your complete import costs
//         </CardDescription>
//       </CardHeader>
//       <CardContent className="space-y-6">
//         {/* Progress Bar */}
//         <div className="space-y-2">
//           <div className="flex justify-between text-sm">
//             <span>Progress</span>
//             <span>{Math.round(progress)}% Complete</span>
//           </div>
//           <Progress value={progress} className="w-full" />
//         </div>
//
//         {/* Step Navigation */}
//         <div className="flex flex-wrap gap-2">
//           {steps.map((step, index) => {
//             const IconComponent = step.icon;
//             return (
//               <Button
//                 key={step.id}
//                 variant={step.active ? "default" : step.completed ? "secondary" : "outline"}
//                 size="sm"
//                 onClick={() => goToStep(index)}
//                 className="flex items-center gap-2"
//               >
//                 {step.completed ? (
//                   <CheckCircle className="h-4 w-4" />
//                 ) : (
//                   <IconComponent className="h-4 w-4" />
//                 )}
//                 <span className="hidden sm:inline">{step.title}</span>
//               </Button>
//             );
//           })}
//         </div>
//
//         <Separator />
//
//         {/* Current Step Content */}
//         <div className="min-h-96">
//           {currentStep === 0 && (
//             <div className="space-y-4">
//               <div className="flex items-center gap-2 mb-4">
//                 <Package className="h-5 w-5" />
//                 <h3 className="font-medium">Step 1: Select Product</h3>
//               </div>
//               <p className="text-muted-foreground mb-4">
//                 Choose your product from our database or enter custom product details including HS code classification.
//               </p>
//               {workflowData.product ? (
//                 <div className="p-4 bg-muted rounded-lg">
//                   <h4 className="font-medium mb-2">Selected Product</h4>
//                   <div className="space-y-1 text-sm">
//                     <p><strong>Name:</strong> {workflowData.product.name}</p>
//                     <p><strong>HS Code:</strong> {workflowData.product.hsCode}</p>
//                     <p><strong>Unit Value:</strong> ${workflowData.product.value}</p>
//                     <p><strong>Quantity:</strong> {workflowData.product.quantity}</p>
//                     <p><strong>Total Value:</strong> ${(workflowData.product.value * workflowData.product.quantity).toLocaleString()}</p>
//                   </div>
//                 </div>
//               ) }
//             </div>
//           )}
//
//           {currentStep === 1 && (
//             <div className="space-y-4">
//               <div className="flex items-center gap-2 mb-4">
//                 <Circle className="h-5 w-5" />
//                 <h3 className="font-medium">Step 2: Country of Origin</h3>
//               </div>
//               <p className="text-muted-foreground mb-4">
//                 Specify where your product was manufactured. This affects duty rates and trade agreement eligibility.
//               </p>
//               {workflowData.origin ? (
//                 <div className="p-4 bg-muted rounded-lg">
//                   <h4 className="font-medium mb-2">Origin Details</h4>
//                   <div className="space-y-1 text-sm">
//                     <p><strong>Country:</strong> {workflowData.origin.country}</p>
//                     <p><strong>Manufacturer:</strong> {workflowData.origin.manufacturer}</p>
//                   </div>
//                 </div>
//               )}
//             </div>
//           )}
//
//           {currentStep === 2 && (
//             <div className="space-y-4">
//               <div className="flex items-center gap-2 mb-4">
//                 <Ship className="h-5 w-5" />
//                 <h3 className="font-medium">Step 3: Shipping Details</h3>
//               </div>
//               <p className="text-muted-foreground mb-4">
//                 Calculate shipping costs based on weight, dimensions, and transportation method.
//               </p>
//               {workflowData.shipping ? (
//                 <div className="p-4 bg-muted rounded-lg">
//                   <h4 className="font-medium mb-2">Shipping Details</h4>
//                   <div className="space-y-1 text-sm">
//                     <p><strong>Method:</strong> {workflowData.shipping.method}</p>
//                     <p><strong>Cost:</strong> ${workflowData.shipping.cost.toLocaleString()}</p>
//                   </div>
//                 </div>
//               )}
//             </div>
//           )}
//
//           {currentStep === 3 && (
//             <div className="space-y-4">
//               <div className="flex items-center gap-2 mb-4">
//                 <Calculator className="h-5 w-5" />
//                 <h3 className="font-medium">Step 4: Calculate Duties</h3>
//               </div>
//               <p className="text-muted-foreground mb-4">
//                 Determine applicable tariff rates and calculate customs duties based on your product and origin country.
//               </p>
//                 {customsResult ? (
//                     <div className="p-4 bg-muted rounded-lg">
//                         <h4 className="font-medium mb-2">Duty Calculation</h4>
//                         <div className="space-y-1 text-sm">
//                             <p><strong>Duty Rate:</strong> {customsResult.finalTariffRate.toFixed(2)}%</p>
//                             <p><strong>Duty Amount:</strong> ${customsResult.dutyAmount.toFixed(2)}</p>
//                         </div>
//                     </div>
//                 ) : (
//                     <CustomsDutyCalculator setResultExternal={handleCustomsResult} />
//                 )}
//             </div>
//           )}
//
//           {currentStep === 4 && (
//             <div className="space-y-4">
//               <div className="flex items-center gap-2 mb-4">
//                 <FileText className="h-5 w-5" />
//                 <h3 className="font-medium">Step 5: Certificate of Origin</h3>
//               </div>
//               <p className="text-muted-foreground mb-4">
//                 Upload and validate your Certificate of Origin for preferential duty rates and customs clearance.
//               </p>
//               {workflowData.certificate?.validated ? (
//                 <div className="p-4 bg-muted rounded-lg">
//                   <h4 className="font-medium mb-2">Certificate Status</h4>
//                   <div className="space-y-1 text-sm">
//                     <p className="flex items-center gap-2">
//                       <CheckCircle className="h-4 w-4 text-green-600" />
//                       <strong>Status:</strong> Validated
//                     </p>
//                     <p><strong>Uploaded:</strong> Yes</p>
//                   </div>
//                 </div>
//               )}
//             </div>
//           )}
//
//           {currentStep === 5 && (
//             <div className="space-y-4">
//               <div className="flex items-center gap-2 mb-4">
//                 <DollarSign className="h-5 w-5" />
//                 <h3 className="font-medium">Step 6: Final Summary</h3>
//               </div>
//               <p className="text-muted-foreground mb-4">
//                 Review your complete import cost breakdown including all fees, duties, and taxes.
//               </p>
//
//               <div className="space-y-4">
//                 <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
//                   <div className="p-4 bg-muted rounded-lg">
//                     <h4 className="font-medium mb-3">Cost Breakdown</h4>
//                     <div className="space-y-2 text-sm">
//                       <div className="flex justify-between">
//                         <span>Product Value:</span>
//                         <span>${(workflowData.product?.value || 0).toLocaleString()}</span>
//                       </div>
//                       <div className="flex justify-between">
//                         <span>Shipping Cost:</span>
//                         <span>${(workflowData.shipping?.cost || 0).toLocaleString()}</span>
//                       </div>
//                       <div className="flex justify-between">
//                         <span>Customs Duty:</span>
//                           <span>
//                             ${(
//                               customsResult?.dutyAmount ??
//                               workflowData.duties?.amount ??
//                               0
//                           ).toFixed(2)}
//                           </span>
//                       </div>
//                       <Separator />
//                       <div className="flex justify-between font-medium">
//                         <span>Total Landed Cost:</span>
//                           <Badge variant="default" className="text-base px-3 py-1">
//                               ${(workflowData.totalCost || 0).toFixed(2)}
//                           </Badge>
//                       </div>
//                     </div>
//                   </div>
//
//                   <div className="p-4 bg-muted rounded-lg">
//                     <h4 className="font-medium mb-3">Summary</h4>
//                     <div className="space-y-2 text-sm">
//                       <p><strong>Product:</strong> {workflowData.product?.name}</p>
//                       <p><strong>Origin:</strong> {workflowData.origin?.country}</p>
//                       <p><strong>Shipping:</strong> {workflowData.shipping?.method}</p>
//                       <p><strong>Certificate:</strong> {workflowData.certificate?.validated ? 'Validated' : 'Not provided'}</p>
//                     </div>
//                   </div>
//                 </div>
//               </div>
//                 <TotalLandedCostCalculator
//                     customsResults={customsResult}
//                     shippingResults={workflowData.shipping}
//                 />
//             </div>
//           )}
//         </div>
//
//         {/* Navigation Buttons */}
//         <div className="flex justify-between pt-4">
//           <Button
//             variant="outline"
//             onClick={prevStep}
//             disabled={currentStep === 0}
//           >
//             Previous
//           </Button>
//           <Button
//             onClick={nextStep}
//             disabled={currentStep === steps.length - 1}
//           >
//             Next
//             <ArrowRight className="h-4 w-4 ml-2" />
//           </Button>
//         </div>
//       </CardContent>
//     </Card>
//   );
// }

import { useState } from "react";
import { CustomsDutyCalculator } from "./CustomsDutyCalculator";
import { TotalLandedCostCalculator } from "./TotalLandedCostCalculator";

export function TariffWorkflow() {
    const [customsResult, setCustomsResult] = useState<any>(null);

    const handleCustomsResult = (result: any) => {
        //console.log("Received customs result from calculator:", result);
        setCustomsResult(result);
    };

    const shippingResult = { shippingCost: 100 }; // mock

    return (
        <div className="space-y-8 p-6">
            <h2 className="text-2xl font-bold mb-2">Tariff & Total Landed Cost</h2>

            <CustomsDutyCalculator onResultsChange={handleCustomsResult} />

            <TotalLandedCostCalculator
                customsResults={customsResult}
                shippingResults={shippingResult}
            />
        </div>
    );
}
