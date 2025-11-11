import { useState } from "react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "./ui/card";
import { Input } from "./ui/input";
import { Label } from "./ui/label";
import { Button } from "./ui/button";
import { Badge } from "./ui/badge";
import { Textarea } from "./ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "./ui/select";
import { Upload, FileText, CheckCircle, AlertCircle, X, Download, Eye } from "lucide-react";
import { Alert, AlertDescription } from "./ui/alert";

interface CertificateData {
  id: string;
  fileName: string;
  fileSize: string;
  uploadDate: string;
  certificateNumber: string;
  issuingAuthority: string;
  issueDate: string;
  expiryDate: string;
  manufacturerName: string;
  manufacturerAddress: string;
  productDescription: string;
  hsCode: string;
  originCountry: string;
  validationStatus: "pending" | "valid" | "invalid" | "expired";
  validationDetails?: string;
}

export function CertificateOfOrigin() {
  const [certificates, setCertificates] = useState<CertificateData[]>([]);
  const [dragActive, setDragActive] = useState(false);
  const [newCertificate, setNewCertificate] = useState({
    certificateNumber: "",
    issuingAuthority: "",
    issueDate: "",
    expiryDate: "",
    manufacturerName: "",
    manufacturerAddress: "",
    productDescription: "",
    hsCode: "",
    originCountry: "",
  });

  const issuingAuthorities = [
    "Chamber of Commerce",
    "Export Promotion Council",
    "Customs Department",
    "Ministry of Trade",
    "Industry Association",
    "Government Trade Office"
  ];

  const countries = [
    "United States", "Canada", "Mexico", "China", "Japan", "South Korea",
    "Germany", "France", "United Kingdom", "Italy", "Spain", "India",
    "Brazil", "Australia", "Singapore", "Thailand", "Vietnam"
  ];

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const handleFileInput = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      handleFiles(e.target.files);
    }
  };

  const handleFiles = (files: FileList) => {
    Array.from(files).forEach(file => {
      // Simulate file upload and basic validation
      const certificate: CertificateData = {
        id: Date.now().toString() + Math.random().toString(36).substr(2, 9),
        fileName: file.name,
        fileSize: `${(file.size / 1024 / 1024).toFixed(2)} MB`,
        uploadDate: new Date().toLocaleDateString(),
        certificateNumber: "",
        issuingAuthority: "",
        issueDate: "",
        expiryDate: "",
        manufacturerName: "",
        manufacturerAddress: "",
        productDescription: "",
        hsCode: "",
        originCountry: "",
        validationStatus: "pending"
      };
      
      setCertificates(prev => [...prev, certificate]);
    });
  };

  const validateCertificate = (id: string) => {
    setCertificates(prev => prev.map(cert => {
      if (cert.id === id) {
        // Simulate validation process
        const now = new Date();
        const expiryDate = new Date(cert.expiryDate);
        const issueDate = new Date(cert.issueDate);
        
        let status: "valid" | "invalid" | "expired" = "valid";
        let details = "Certificate validated successfully";
        
        if (!cert.certificateNumber || !cert.issuingAuthority || !cert.originCountry) {
          status = "invalid";
          details = "Missing required certificate information";
        } else if (expiryDate < now) {
          status = "expired";
          details = "Certificate has expired";
        } else if (issueDate > now) {
          status = "invalid";
          details = "Issue date cannot be in the future";
        }
        
        return {
          ...cert,
          validationStatus: status,
          validationDetails: details
        };
      }
      return cert;
    }));
  };

  const updateCertificate = (id: string, updates: Partial<CertificateData>) => {
    setCertificates(prev => prev.map(cert => 
      cert.id === id ? { ...cert, ...updates } : cert
    ));
  };

  const removeCertificate = (id: string) => {
    setCertificates(prev => prev.filter(cert => cert.id !== id));
  };

  const getStatusIcon = (status: CertificateData['validationStatus']) => {
    switch (status) {
      case "valid":
        return <CheckCircle className="h-4 w-4 text-green-600" />;
      case "invalid":
      case "expired":
        return <AlertCircle className="h-4 w-4 text-red-600" />;
      default:
        return <AlertCircle className="h-4 w-4 text-yellow-600" />;
    }
  };

  const getStatusBadge = (status: CertificateData['validationStatus']) => {
    const variants = {
      valid: "default",
      invalid: "destructive",
      expired: "destructive",
      pending: "secondary"
    } as const;
    
    return (
      <Badge variant={variants[status]}>
        {status.charAt(0).toUpperCase() + status.slice(1)}
      </Badge>
    );
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <FileText className="h-5 w-5" />
          Certificate of Origin
        </CardTitle>
        <CardDescription>
          Upload and validate certificates of origin for customs clearance
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-6">
        {/* File Upload Area */}
          <div
              className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
                  dragActive ? "border-primary bg-primary/5" : "border-border"
              }`}
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
        >
          <Upload className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
          <h3 className="font-medium mb-2">Upload Certificate of Origin</h3>
          <p className="text-sm text-muted-foreground mb-4">
            Drag and drop your certificate files here, or click to browse
          </p>
          <input
            type="file"
            multiple
            accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
            onChange={handleFileInput}
            className="hidden"
            id="certificate-upload"
          />
          <Button asChild variant="outline">
            <label htmlFor="certificate-upload" className="cursor-pointer">
              Browse Files
            </label>
          </Button>
          <p className="text-xs text-muted-foreground mt-2">
            Supported formats: PDF, JPG, PNG, DOC, DOCX (Max 10MB each)
          </p>
        </div>

        {/* Certificate List */}
        {certificates.length > 0 && (
          <div className="space-y-4">
            <h3 className="font-medium">Uploaded Certificates</h3>
            
            {certificates.map((certificate) => (
              <div key={certificate.id} className="border rounded-lg p-4 space-y-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <FileText className="h-5 w-5 text-muted-foreground" />
                    <div>
                      <h4 className="font-medium">{certificate.fileName}</h4>
                      <p className="text-sm text-muted-foreground">
                        {certificate.fileSize} • Uploaded {certificate.uploadDate}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    {getStatusIcon(certificate.validationStatus)}
                    {getStatusBadge(certificate.validationStatus)}
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => removeCertificate(certificate.id)}
                    >
                      <X className="h-4 w-4" />
                    </Button>
                  </div>
                </div>

                {certificate.validationStatus !== "pending" && certificate.validationDetails && (
                  <Alert>
                    <AlertCircle className="h-4 w-4" />
                    <AlertDescription>{certificate.validationDetails}</AlertDescription>
                  </Alert>
                )}

                {/* Certificate Details Form */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor={`cert-number-${certificate.id}`}>Certificate Number</Label>
                    <Input
                      id={`cert-number-${certificate.id}`}
                      value={certificate.certificateNumber}
                      onChange={(e) => updateCertificate(certificate.id, { certificateNumber: e.target.value })}
                      placeholder="Enter certificate number"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`issuing-authority-${certificate.id}`}>Issuing Authority</Label>
                    <Select
                      value={certificate.issuingAuthority}
                      onValueChange={(value) => updateCertificate(certificate.id, { issuingAuthority: value })}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select issuing authority" />
                      </SelectTrigger>
                      <SelectContent>
                        {issuingAuthorities.map((authority) => (
                          <SelectItem key={authority} value={authority}>
                            {authority}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`issue-date-${certificate.id}`}>Issue Date</Label>
                    <Input
                      id={`issue-date-${certificate.id}`}
                      type="date"
                      value={certificate.issueDate}
                      onChange={(e) => updateCertificate(certificate.id, { issueDate: e.target.value })}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`expiry-date-${certificate.id}`}>Expiry Date</Label>
                    <Input
                      id={`expiry-date-${certificate.id}`}
                      type="date"
                      value={certificate.expiryDate}
                      onChange={(e) => updateCertificate(certificate.id, { expiryDate: e.target.value })}
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`manufacturer-${certificate.id}`}>Manufacturer Name</Label>
                    <Input
                      id={`manufacturer-${certificate.id}`}
                      value={certificate.manufacturerName}
                      onChange={(e) => updateCertificate(certificate.id, { manufacturerName: e.target.value })}
                      placeholder="Enter manufacturer name"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label htmlFor={`origin-country-${certificate.id}`}>Country of Origin</Label>
                    <Select
                      value={certificate.originCountry}
                      onValueChange={(value) => updateCertificate(certificate.id, { originCountry: value })}
                    >
                      <SelectTrigger>
                        <SelectValue placeholder="Select country of origin" />
                      </SelectTrigger>
                      <SelectContent>
                        {countries.map((country) => (
                          <SelectItem key={country} value={country}>
                            {country}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>

                  <div className="space-y-2 md:col-span-2">
                    <Label htmlFor={`manufacturer-address-${certificate.id}`}>Manufacturer Address</Label>
                    <Textarea
                      id={`manufacturer-address-${certificate.id}`}
                      value={certificate.manufacturerAddress}
                      onChange={(e) => updateCertificate(certificate.id, { manufacturerAddress: e.target.value })}
                      placeholder="Enter complete manufacturer address"
                    />
                  </div>

                  <div className="space-y-2 md:col-span-2">
                    <Label htmlFor={`product-description-${certificate.id}`}>Product Description</Label>
                    <Textarea
                      id={`product-description-${certificate.id}`}
                      value={certificate.productDescription}
                      onChange={(e) => updateCertificate(certificate.id, { productDescription: e.target.value })}
                      placeholder="Enter detailed product description"
                    />
                  </div>
                </div>

                <div className="flex gap-2 pt-2">
                  <Button
                    onClick={() => validateCertificate(certificate.id)}
                    variant="default"
                    size="sm"
                  >
                    <CheckCircle className="h-4 w-4 mr-2" />
                    Validate Certificate
                  </Button>
                  <Button variant="outline" size="sm">
                    <Eye className="h-4 w-4 mr-2" />
                    Preview
                  </Button>
                  <Button variant="outline" size="sm">
                    <Download className="h-4 w-4 mr-2" />
                    Download
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}

        {certificates.length === 0 && (
          <div className="text-center py-8 text-muted-foreground">
            No certificates uploaded yet. Upload your Certificate of Origin to get started.
          </div>
        )}

        {/* Information Section */}
        <div className="bg-muted p-4 rounded-lg">
          <h4 className="font-medium mb-2">About Certificate of Origin</h4>
          <p className="text-sm text-muted-foreground mb-2">
            A Certificate of Origin is a document that certifies the country where goods were manufactured. 
            It's often required for customs clearance and may affect duty rates under trade agreements.
          </p>
          <div className="space-y-1 text-sm text-muted-foreground">
            <p>• Required for preferential duty rates under trade agreements</p>
            <p>• Must be issued by an authorized chamber of commerce or government agency</p>
            <p>• Should include detailed product description and manufacturer information</p>
            <p>• Check expiry dates to ensure validity at time of import</p>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}