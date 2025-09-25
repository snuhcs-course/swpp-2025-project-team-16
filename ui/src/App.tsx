import { useState } from "react";
import { Calendar } from "./components/ui/calendar";
import { Button } from "./components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "./components/ui/card";
import { Progress } from "./components/ui/progress";
import { Badge } from "./components/ui/badge";

export default function App() {
  const [date, setDate] = useState<Date | undefined>(new Date());
  
  // Mock data for achievement rate
  const achievementRate = 68;
  const currentMonth = new Date().toLocaleDateString('ko-KR', { 
    year: 'numeric', 
    month: 'long' 
  });

  return (
    <div className="min-h-screen bg-background p-4">
      <div className="max-w-md mx-auto space-y-6">
        {/* Header */}
        <div className="text-center py-4">
          <h1 className="text-3xl mb-2">운동 계획</h1>
          <Badge variant="secondary" className="px-3 py-1">
            {currentMonth}
          </Badge>
        </div>

        {/* Calendar */}
        <Card>
          <CardHeader>
            <CardTitle className="text-center">이번 달 달력</CardTitle>
          </CardHeader>
          <CardContent className="flex justify-center">
            <Calendar
              mode="single"
              selected={date}
              onSelect={setDate}
              className="rounded-md border"
            />
          </CardContent>
        </Card>

        {/* Action Buttons */}
        <div className="grid grid-cols-2 gap-4">
          <Button 
            variant="default" 
            className="w-full py-6"
            onClick={() => alert('계획 생성 기능')}
          >
            계획 생성
          </Button>
          <Button 
            variant="outline" 
            className="w-full py-6"
            onClick={() => alert('계획 수정 기능')}
          >
            계획 수정
          </Button>
        </div>

        {/* Achievement Rate */}
        <Card>
          <CardHeader>
            <CardTitle className="flex justify-between items-center">
              <span>계획 성취율</span>
              <Badge variant="default" className="bg-primary">
                {achievementRate}%
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <Progress value={achievementRate} className="w-full h-3" />
            <div className="text-center text-sm text-muted-foreground">
              이번 달 운동 계획 달성률
            </div>
            <div className="grid grid-cols-3 gap-4 text-center text-sm">
              <div>
                <div className="text-primary">23일</div>
                <div className="text-muted-foreground">완료</div>
              </div>
              <div>
                <div className="text-muted-foreground">11일</div>
                <div className="text-muted-foreground">미완료</div>
              </div>
              <div>
                <div className="text-accent-foreground">34일</div>
                <div className="text-muted-foreground">총 계획</div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}